package com.mintanable.notethepad.features.presentation.modify.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.mintanable.notethepad.features.domain.model.Note
import com.mintanable.notethepad.ui.theme.RedOrange

@Composable
fun NoteItem(
    note: Note,
    modifier:Modifier=Modifier,
    cornerRadius: Dp = 10.dp,
    cutCornerSize : Dp = 30.dp,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier=modifier
    ){
       Canvas(modifier = Modifier.matchParentSize()){
           val clipPath = Path().apply{
               lineTo(size.width-cutCornerSize.toPx(),0f)
               lineTo(size.width, cutCornerSize.toPx())
               lineTo(size.width, size.height)
               lineTo(0f,size.height)
               close()
           }
           clipPath(clipPath){
                drawRoundRect(
                    color = Color(note.color),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )

               drawRoundRect(
                   color = Color(ColorUtils.blendARGB(note.color,0x000000, 0.2f)
                   ),
                   topLeft = Offset(size.width-cutCornerSize.toPx(), -100f),
                   size = Size(cutCornerSize.toPx()+100f,cutCornerSize.toPx()+100f),
                   cornerRadius = CornerRadius(cornerRadius.toPx())
               )
           }
       }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(end = 32.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(alignment = Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete note",
                tint = MaterialTheme.colors.onSurface
            )

        }
    }
}


@Preview(showBackground = true)
@Composable
fun NoteItemPreview() {
    MaterialTheme {
        NoteItem(
            note = Note(
                title = "Meeting Notes",
                content = "Discuss the new architecture for the JioHotstar platform. Focus on performance and scalability.",
                timestamp = System.currentTimeMillis(),
                color = RedOrange.toArgb(),
                id = 1
            ),
            modifier = Modifier.fillMaxWidth(),
            onDeleteClick = {}
        )
    }
}

//@Composable
//fun CanvasTest(){
//    Canvas(
//        modifier = Modifier
//            .padding(16.dp)
//            .size(300.dp)
//    ) {
//        drawRect(
//            color = Color.Blue,
//            size = size
//        )
//
//        drawRect(
//            color = Color.Red,
//            topLeft = Offset(50f,50f),
//            size = Size(100f,100f),
//            style = Stroke(width = 3.dp.toPx())
//        )
//
//        drawCircle(
//            brush = Brush.radialGradient(
//                colors =
//                    listOf(Color.Blue, Color.Green, Color.Red, Color.Yellow),
//                center = center,
//                radius = 100f
//            ),
//            radius = 100f,
//        )
//
//        drawArc(
//            color = Color.Magenta,
//            startAngle= 0f,
//            sweepAngle = 270f,
//            useCenter = true,
//            topLeft = Offset(100f,500f),
//            size = Size(200f,200f)
//        )
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewCanvaseTest(){
//    MaterialTheme {
//    CanvasTest()
//        }
//}