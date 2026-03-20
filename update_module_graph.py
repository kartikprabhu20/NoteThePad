#!/usr/bin/env python3
"""
update_module_graph.py
Parses Gradle build files and regenerates module-graph.html.

Usage (from project root or any subdirectory):
    python3 update_module_graph.py
"""
import os, re, sys
from pathlib import Path
from collections import defaultdict, deque

# ── Gradle parsing ────────────────────────────────────────────────────────────

def find_root():
    p = Path(__file__).resolve().parent
    while p != p.parent:
        if (p / 'settings.gradle').exists() or (p / 'settings.gradle.kts').exists():
            return p
        p = p.parent
    sys.exit('ERROR: No settings.gradle found. Run from inside an Android project.')

def parse_includes(root):
    for name in ('settings.gradle', 'settings.gradle.kts'):
        f = root / name
        if f.exists():
            return re.findall(r"include\s+['\"]([^'\"]+)['\"]", f.read_text())
    return []

def build_file(root, mid):
    parts = mid.lstrip(':').split(':')
    for ext in ('build.gradle', 'build.gradle.kts'):
        p = root.joinpath(*parts) / ext
        if p.exists():
            return p
    return None

def parse_module_deps(gradle_path):
    """Return list of (dep_module_id, is_api) for project() dependencies."""
    text = gradle_path.read_text()
    # Strip single-line comments so commented-out deps don't appear as edges
    text = re.sub(r'//[^\n]*', '', text)
    results = []
    pat = r'(api|implementation)\s*\(?\s*project\s*\(\s*[\'"]([^\'"]+)[\'"]\s*\)\s*\)?'
    for m in re.finditer(pat, text):
        results.append((m.group(2), m.group(1) == 'api'))
    return results

def mod_type(mid):
    s = mid.lstrip(':')
    if s == 'app':            return 'app'
    if s.startswith('core:'): return 'core'
    return 'feature'

def mod_label(mid):
    return mid if mid.startswith(':') else ':' + mid

# ── Auto layout ───────────────────────────────────────────────────────────────

def compute_layout(all_ids, edges):
    """
    BFS from graph roots (modules no-one depends on).
    Row  = BFS depth (roots at row 0).
    Column = sorted within row by type (feature before core) then name.
    Returns dict: mid → (col_float, row_int)
    """
    out = defaultdict(set)   # mid → set of deps
    ins = defaultdict(set)   # mid → set of dependents

    for f, t, _ in edges:
        if f in all_ids and t in all_ids:
            out[f].add(t)
            ins[t].add(f)

    roots = [i for i in all_ids if not ins.get(i)] or [all_ids[0]]

    # Longest-path depth (so every node is below all its dependents)
    depth = {i: 0 for i in all_ids}
    q = deque(roots)
    visited = set(roots)
    while q:
        cur = q.popleft()
        for dep in out.get(cur, []):
            if depth[dep] < depth[cur] + 1:
                depth[dep] = depth[cur] + 1
            if dep not in visited:
                visited.add(dep)
                q.append(dep)

    rows = defaultdict(list)
    for mid in all_ids:
        rows[depth[mid]].append(mid)

    type_order = {'app': 0, 'feature': 1, 'core': 2}
    for d in rows:
        rows[d].sort(key=lambda x: (type_order.get(mod_type(x), 9), x))

    pos = {}
    sorted_depths = sorted(rows.keys())
    max_cols = max(len(rows[d]) for d in sorted_depths)
    for ri, d in enumerate(sorted_depths):
        mods = rows[d]
        offset = (max_cols - len(mods)) / 2.0
        for ci, mid in enumerate(mods):
            pos[mid] = (offset + ci, ri)

    return pos

# ── Data section renderer ─────────────────────────────────────────────────────

def render_data(modules, edges, pos):
    COL, ROW, PX, PY = 168, 120, 36, 48

    def fmt(v):
        return f'{v:.1f}' if v != int(v) else str(int(v))

    lines = []

    # MODULES
    lines.append('const MODULES = [')
    for m in modules:
        lines.append(f"  {{ id: '{m['id']}', label: '{m['label']}', type: '{m['type']}' }},")
    lines.append('];\n')

    # EDGES
    lines.append('// [from, to, isApi]')
    lines.append('const EDGES = [')
    for f, t, api in edges:
        fp = (f"'{f}',").ljust(32)
        tp = (f"'{t}',").ljust(30)
        lines.append(f"  [{fp} {tp} {'true' if api else 'false'}],")
    lines.append('];\n')

    # Layout constants + DEFAULT_POS
    lines.append(f'const NODE_W = 152, NODE_H = 36;')
    lines.append(f'const COL = {COL}, ROW = {ROW}, PX = {PX}, PY = {PY};\n')
    lines.append('const DEFAULT_POS = {')
    for m in modules:
        mid = m['id']
        col, row = pos.get(mid, (0, 0))
        key = (f"'{mid}':").ljust(34)
        lines.append(f'  {key} {{ col: {fmt(col)}, row: {fmt(row)} }},')
    lines.append('};\n')

    lines.append('function defaultXY(id) {')
    lines.append('  const d = DEFAULT_POS[id];')
    lines.append('  return { x: PX + d.col * COL, y: PY + d.row * ROW };')
    lines.append('}\n')
    lines.append('const pos = {};')
    lines.append('MODULES.forEach(m => { pos[m.id] = { ...defaultXY(m.id) }; });\n')
    lines.append('function cx(id) { return pos[id].x + NODE_W / 2; }')
    lines.append('function cy(id) { return pos[id].y + NODE_H / 2; }')

    return '\n'.join(lines)

# ── HTML template ─────────────────────────────────────────────────────────────

HEADER = '''\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Module Dependency Graph</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: #0d1117; color: #c9d1d9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', monospace; overflow: hidden; user-select: none; }
  #canvas-wrap { width: 100vw; height: 100vh; position: relative; }
  svg { width: 100%; height: 100%; }

  .node rect { stroke-width: 1.5; filter: drop-shadow(0 2px 6px rgba(0,0,0,.5)); }
  .node text { font-size: 12px; font-weight: 500; fill: #e6edf3; pointer-events: none; }
  .node.app     rect { fill: #1a1f35; stroke: #7c3aed; }
  .node.feature rect { fill: #0d2136; stroke: #1f6feb; }
  .node.core    rect { fill: #0d2014; stroke: #238636; }

  .edge         { fill: none; stroke: #30363d; stroke-width: 1.5; }
  .edge.api     { stroke: #238636; stroke-dasharray: 6 3; }
  .edge.dim     { opacity: .15; }
  .edge.hi      { stroke: #58a6ff; stroke-width: 2.5; }
  .edge.hi.api  { stroke: #3fb950; }

  #controls {
    position: absolute; top: 14px; right: 14px;
    display: flex; flex-direction: column; gap: 6px; z-index: 30;
  }
  #controls button {
    background: #21262d; border: 1px solid #30363d; color: #c9d1d9;
    padding: 7px 16px; border-radius: 6px; cursor: pointer; font-size: 12px; text-align: left;
  }
  #controls button:hover { background: #30363d; border-color: #58a6ff; }

  #legend {
    position: absolute; bottom: 14px; left: 14px;
    background: #161b22; border: 1px solid #30363d;
    border-radius: 8px; padding: 12px 16px; font-size: 12px; z-index: 30; line-height: 2;
  }
  #legend .row { display: flex; align-items: center; gap: 8px; }
  .sw { width: 28px; height: 3px; border-radius: 2px; }
  .sw.app  { background: #7c3aed; }
  .sw.feat { background: #1f6feb; }
  .sw.core { background: #238636; }

  #tip {
    position: absolute; background: #161b22; border: 1px solid #30363d;
    border-radius: 8px; padding: 10px 14px; font-size: 12px;
    pointer-events: none; opacity: 0; transition: opacity .12s;
    max-width: 300px; z-index: 40; line-height: 1.65;
  }
  #tip .title { font-weight: 700; color: #e6edf3; margin-bottom: 5px; font-size: 13px; }
  #tip .dep   { color: #8b949e; }
  #tip .dep span { color: #58a6ff; }

  #hint {
    position: absolute; top: 14px; left: 50%; transform: translateX(-50%);
    font-size: 11px; color: #484f58; pointer-events: none; white-space: nowrap;
  }
</style>
</head>
<body>
<div id="canvas-wrap">
  <svg id="svg" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <marker id="ar"        markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#30363d"/></marker>
      <marker id="ar-hi"     markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#58a6ff"/></marker>
      <marker id="ar-hi-api" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#3fb950"/></marker>
      <marker id="ar-api"    markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L8,3 z" fill="#238636"/></marker>
    </defs>
    <g id="edges-layer"></g>
    <g id="nodes-layer"></g>
  </svg>
  <div id="tip"></div>
  <div id="controls">
    <button onclick="fitView()">&#x229f; Fit view</button>
    <button onclick="resetLayout()">&#x21ba; Reset layout</button>
    <button onclick="resetHighlight()">&#x2715; Clear highlight</button>
  </div>
  <div id="legend">
    <div class="row"><div class="sw app"></div> :app</div>
    <div class="row"><div class="sw feat"></div> :feature:*</div>
    <div class="row"><div class="sw core"></div> :core:*</div>
    <div class="row" style="gap:8px;align-items:center">
      <div style="width:28px;border-top:2px dashed #238636"></div> api dep
    </div>
  </div>
  <div id="hint">Drag nodes &middot; Scroll to zoom &middot; Drag background to pan &middot; Click node to highlight</div>
</div>

<script>
// ── AUTO-GENERATED — edit update_module_graph.py, not this file ───────────────
'''

FOOTER = '''
// ── Adjacency ─────────────────────────────────────────────────────────────────
const deps = {};
MODULES.forEach(m => { deps[m.id] = []; });
EDGES.forEach(([f, t, api]) => deps[f].push({ id: t, api }));

// ── SVG helpers ───────────────────────────────────────────────────────────────
const svgNS = 'http://www.w3.org/2000/svg';
function mkEl(tag, attrs) {
  const e = document.createElementNS(svgNS, tag);
  Object.entries(attrs).forEach(([k, v]) => e.setAttribute(k, v));
  return e;
}

const svg        = document.getElementById('svg');
const edgesLayer = document.getElementById('edges-layer');
const nodesLayer = document.getElementById('nodes-layer');

let vx = 0, vy = 0, scale = 1;
let panActive = false, panLx, panLy;

const viewport = mkEl('g', {});
svg.insertBefore(viewport, edgesLayer);
viewport.appendChild(edgesLayer);
viewport.appendChild(nodesLayer);

function applyVP() {
  viewport.setAttribute('transform', `translate(${vx},${vy}) scale(${scale})`);
}

// ── Edges ─────────────────────────────────────────────────────────────────────
function edgeD(f, t) {
  const fx = cx(f), fy = cy(f), tx = cx(t), ty = cy(t);
  const dx = tx - fx, dy = ty - fy, len = Math.hypot(dx, dy);
  if (len < 1) return '';
  const nx = dx / len, ny = dy / len;
  const sx = fx + nx * (NODE_W / 2 + 2),  sy = fy + ny * (NODE_H / 2 + 2);
  const ex = tx - nx * (NODE_W / 2 + 7),  ey = ty - ny * (NODE_H / 2 + 7);
  return `M${sx.toFixed(1)},${sy.toFixed(1)} L${ex.toFixed(1)},${ey.toFixed(1)}`;
}

const edgeEls = EDGES.map(([f, t, api]) => {
  const path = mkEl('path', {
    class: 'edge' + (api ? ' api' : ''),
    'marker-end': api ? 'url(#ar-api)' : 'url(#ar)',
    d: edgeD(f, t),
  });
  edgesLayer.appendChild(path);
  return { el: path, from: f, to: t, api };
});

function refreshEdges(nodeId) {
  edgeEls.forEach(e => {
    if (!nodeId || e.from === nodeId || e.to === nodeId)
      e.el.setAttribute('d', edgeD(e.from, e.to));
  });
}

// ── Nodes ─────────────────────────────────────────────────────────────────────
const nodeEls   = {};
const nodeTypes = {};

MODULES.forEach(m => {
  nodeTypes[m.id] = m.type;
  const g = mkEl('g', {
    class: `node ${m.type}`,
    transform: `translate(${pos[m.id].x},${pos[m.id].y})`,
    'data-nid': m.id,
  });
  g.style.cursor = 'grab';
  g.appendChild(mkEl('rect', { width: NODE_W, height: NODE_H }));
  const txt = document.createElementNS(svgNS, 'text');
  txt.setAttribute('x', NODE_W / 2);
  txt.setAttribute('y', NODE_H / 2 + 4);
  txt.setAttribute('text-anchor', 'middle');
  txt.textContent = m.label;
  g.appendChild(txt);
  g.addEventListener('mouseenter', e => showTip(m.id, e));
  g.addEventListener('mousemove',  e => moveTip(e));
  g.addEventListener('mouseleave', hideTip);
  nodesLayer.appendChild(g);
  nodeEls[m.id] = g;
});

// ── Drag + Pan + Click — all handled from SVG level (event delegation) ────────
let dragNode = null, dragLx, dragLy, dragMoved = false;

function nodeOf(e) {
  // Walk up from e.target to find a node <g data-nid="...">
  let el = e.target;
  while (el && el !== svg) {
    if (el.dataset && el.dataset.nid) return el;
    el = el.parentElement;
  }
  return null;
}

svg.addEventListener('mousedown', e => {
  const node = nodeOf(e);
  if (node) {
    dragNode  = node.dataset.nid;
    dragLx    = e.clientX;
    dragLy    = e.clientY;
    dragMoved = false;
    node.style.cursor = 'grabbing';
    nodesLayer.appendChild(node);   // bring to front
  } else {
    panActive = true;
    panLx = e.clientX;
    panLy = e.clientY;
    svg.style.cursor = 'grabbing';
  }
  e.preventDefault();
});

window.addEventListener('mousemove', e => {
  if (dragNode) {
    const dx = (e.clientX - dragLx) / scale;
    const dy = (e.clientY - dragLy) / scale;
    pos[dragNode].x += dx;
    pos[dragNode].y += dy;
    dragLx = e.clientX;
    dragLy = e.clientY;
    dragMoved = true;
    nodeEls[dragNode].setAttribute('transform', `translate(${pos[dragNode].x},${pos[dragNode].y})`);
    refreshEdges(dragNode);
    return;
  }
  if (panActive) {
    vx += e.clientX - panLx;
    vy += e.clientY - panLy;
    panLx = e.clientX;
    panLy = e.clientY;
    applyVP();
  }
});

window.addEventListener('mouseup', e => {
  if (dragNode) {
    nodeEls[dragNode].style.cursor = 'grab';
    if (!dragMoved) toggleHighlight(dragNode);   // click = no movement
    dragNode = null;
    dragMoved = false;
  }
  if (panActive) {
    panActive = false;
    svg.style.cursor = '';
  }
});

// Background click (mouseup with no drag and no node) = clear highlight
svg.addEventListener('click', e => {
  if (!nodeOf(e)) resetHighlight();
});

// ── Zoom ──────────────────────────────────────────────────────────────────────
svg.addEventListener('wheel', e => {
  e.preventDefault();
  const r = svg.getBoundingClientRect();
  const mx = e.clientX - r.left, my = e.clientY - r.top;
  const factor = e.deltaY < 0 ? 1.12 : 0.89;
  const ns = Math.max(0.15, Math.min(5, scale * factor));
  vx = mx - (mx - vx) * (ns / scale);
  vy = my - (my - vy) * (ns / scale);
  scale = ns;
  applyVP();
}, { passive: false });

// ── Fit / Reset ───────────────────────────────────────────────────────────────
function fitView() {
  const W = svg.clientWidth, H = svg.clientHeight;
  const xs = MODULES.map(m => pos[m.id].x), ys = MODULES.map(m => pos[m.id].y);
  const minX = Math.min(...xs) - 20, minY = Math.min(...ys) - 20;
  const maxX = Math.max(...xs) + NODE_W + 20, maxY = Math.max(...ys) + NODE_H + 20;
  scale = Math.min(W / (maxX - minX), H / (maxY - minY)) * 0.92;
  vx = (W - (maxX + minX) * scale) / 2;
  vy = (H - (maxY + minY) * scale) / 2;
  applyVP();
}

function resetLayout() {
  MODULES.forEach(m => {
    pos[m.id] = { ...defaultXY(m.id) };
    nodeEls[m.id].setAttribute('transform', `translate(${pos[m.id].x},${pos[m.id].y})`);
  });
  refreshEdges();
  fitView();
  resetHighlight();
}

// ── Highlight ─────────────────────────────────────────────────────────────────
let activeHighlight = null;

// Pre-build forward and reverse adjacency maps
const fwdAdj = {};
MODULES.forEach(m => { fwdAdj[m.id] = deps[m.id].map(d => d.id); });

const rdepsMap = {};
MODULES.forEach(m => { rdepsMap[m.id] = []; });
EDGES.forEach(([f, t]) => rdepsMap[t].push(f));

function bfs(starts, adj) {
  const visited = new Set(starts);
  const q = [...starts];
  while (q.length) {
    (adj[q.shift()] || []).forEach(n => { if (!visited.has(n)) { visited.add(n); q.push(n); } });
  }
  return visited;
}

function toggleHighlight(id) {
  if (activeHighlight === id) { resetHighlight(); return; }
  activeHighlight = id;

  const active = new Set([...bfs([id], fwdAdj), ...bfs([id], rdepsMap)]);

  // Nodes: dim unrelated, highlight connected, mark selected with yellow border
  MODULES.forEach(m => {
    const g   = nodeEls[m.id];
    const r   = g.querySelector('rect');
    if (m.id === id) {
      g.style.opacity = '1';
      r.style.stroke       = '#f5c542';
      r.style.strokeWidth  = '3';
      r.style.filter       = 'drop-shadow(0 0 10px #f5c542)';
    } else if (active.has(m.id)) {
      g.style.opacity = '1';
      r.style.stroke       = '';
      r.style.strokeWidth  = '2';
      r.style.filter       = 'drop-shadow(0 0 6px rgba(255,255,255,0.2))';
    } else {
      g.style.opacity = '0.12';
      r.style.stroke      = '';
      r.style.strokeWidth = '';
      r.style.filter      = '';
    }
  });

  // Edges: highlight those between active nodes, dim the rest
  edgeEls.forEach(({ el, from, to, api }) => {
    if (active.has(from) && active.has(to)) {
      el.style.opacity     = '1';
      el.style.stroke      = api ? '#3fb950' : '#58a6ff';
      el.style.strokeWidth = '2.5';
      el.setAttribute('marker-end', api ? 'url(#ar-hi-api)' : 'url(#ar-hi)');
    } else {
      el.style.opacity     = '0.06';
      el.style.stroke      = '';
      el.style.strokeWidth = '';
      el.setAttribute('marker-end', api ? 'url(#ar-api)' : 'url(#ar)');
    }
  });
}

function resetHighlight() {
  activeHighlight = null;
  MODULES.forEach(m => {
    const g = nodeEls[m.id];
    const r = g.querySelector('rect');
    g.style.opacity      = '';
    r.style.stroke       = '';
    r.style.strokeWidth  = '';
    r.style.filter       = '';
  });
  edgeEls.forEach(({ el, api }) => {
    el.style.opacity     = '';
    el.style.stroke      = '';
    el.style.strokeWidth = '';
    el.setAttribute('marker-end', api ? 'url(#ar-api)' : 'url(#ar)');
  });
}

// ── Tooltip ───────────────────────────────────────────────────────────────────
const tip = document.getElementById('tip');
function showTip(id, e) {
  const d = deps[id];
  const lines = d.length
    ? d.map(dep => `<div class="dep">&rarr; <span>${dep.id}</span>${dep.api ? ' <em style="color:#3fb950">(api)</em>' : ''}</div>`).join('')
    : '<div class="dep" style="color:#484f58">no module deps</div>';
  tip.innerHTML = `<div class="title">${id}</div>${lines}`;
  tip.style.opacity = '1';
  moveTip(e);
}
function moveTip(e) {
  tip.style.left = Math.min(e.clientX + 14, window.innerWidth  - 320) + 'px';
  tip.style.top  = Math.min(e.clientY - 10, window.innerHeight - 200) + 'px';
}
function hideTip() { tip.style.opacity = '0'; }

// ── Init ──────────────────────────────────────────────────────────────────────
fitView();
</script>
</body>
</html>
'''

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    root = find_root()
    print(f'Project root : {root}')

    # Discover modules
    includes = parse_includes(root)
    modules, valid_set = [], set()
    for mid in includes:
        bf = build_file(root, mid)
        if bf:
            modules.append({'id': mid, 'label': mod_label(mid), 'type': mod_type(mid)})
            valid_set.add(mid)
        else:
            print(f'  SKIP (no build.gradle): {mid}', file=sys.stderr)

    print(f'Modules found: {len(modules)}')

    # Collect inter-module edges
    edges = []
    for m in modules:
        bf = build_file(root, m['id'])
        for dep_id, is_api in parse_module_deps(bf):
            if dep_id in valid_set:
                edges.append((m['id'], dep_id, is_api))

    print(f'Edges found  : {len(edges)}')

    # Layout
    all_ids = [m['id'] for m in modules]
    pos = compute_layout(all_ids, edges)

    # Render
    data_js = render_data(modules, edges, pos)
    html = HEADER + data_js + FOOTER

    out = root / 'module-graph.html'
    out.write_text(html, encoding='utf-8')
    print(f'Written      : {out}')

if __name__ == '__main__':
    main()
