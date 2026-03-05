package com.mintanable.notethepad.feature_widgets.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.LazyVerticalGridScope
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import kotlin.math.ceil

/**
 * A variant of [LazyVerticalGrid] that clips its scrolling content to a rounded rectangle.
 *
 * @param gridCells the number of columns in the grid.
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyVerticalGridScope.item] to add a single item or [LazyVerticalGridScope.items] to add a list
 * of items. If the item has more than one top-level child, they will be automatically wrapped in a
 * Box.
 * @see LazyVerticalGrid
 */
@Composable
fun RoundedScrollingLazyVerticalGrid(
  gridCells: GridCells,
  modifier: GlanceModifier = GlanceModifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  content: LazyVerticalGridScope.() -> Unit,
) {
  Box(
    modifier = GlanceModifier
      .cornerRadius(16.dp) // to present a rounded scrolling experience
      .then(modifier)
  ) {
    LazyVerticalGrid(
      gridCells = gridCells,
      horizontalAlignment = horizontalAlignment,
      content = content
    )
  }
}

/**
 * A variant of [LazyVerticalGrid] that clips its scrolling content to a rounded rectangle and
 * spaces out each item in the grid with a default 8.dp spacing.
 *
 * @param gridCells number of columns in the grid
 * @param items the list of data items to be displayed in the list
 * @param itemContentProvider a lambda function that provides item content without any spacing
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param cellSpacing horizontal and vertical spacing between cells
 * @see LazyVerticalGrid
 */
@Composable
fun <T> RoundedScrollingLazyVerticalGrid(
  gridCells: Int,
  items: List<T>,
  itemContentProvider: @Composable (item: T) -> Unit,
  modifier: GlanceModifier = GlanceModifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  cellSpacing: Dp = 4.dp,
) {
  val numRows = ceil(items.size.toDouble() / gridCells).toInt()

  RoundedScrollingLazyVerticalGrid(
    gridCells = GridCells.Fixed(gridCells),
    horizontalAlignment = horizontalAlignment,
    modifier = modifier
  ) {

    itemsIndexed(items) { index, item ->
      val row = index / gridCells
      val column = index % gridCells

      Box(
        modifier = modifier
          .fillMaxSize()
          .padding(
            start = cellSpacing,
            end = if (column == gridCells - 1) cellSpacing else 0.dp,
            top = cellSpacing,
            bottom = if (row == numRows - 1) cellSpacing else 0.dp
          )
      ) {
        itemContentProvider(item)
      }
    }
  }
}