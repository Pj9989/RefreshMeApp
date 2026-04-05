package com.refreshme.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * An interactive image slider that allows users to drag a slider back and forth 
 * to compare a 'Before' image with an 'After' image.
 */
@Composable
fun BeforeAfterImageSlider(
    beforeImageUrl: String,
    afterImageUrl: String,
    modifier: Modifier = Modifier,
    sliderColor: Color = Color.White
) {
    // Start the slider right in the middle
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        
        // 1. Bottom Image (After Image - The final haircut)
        AsyncImage(
            model = afterImageUrl,
            contentDescription = "After image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Image (Before Image - Clipped by the slider position)
        AsyncImage(
            model = beforeImageUrl,
            contentDescription = "Before image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(FractionalWidthShape(sliderPosition))
        )

        // 3. Slider Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sliderX = size.width * sliderPosition
            drawLine(
                color = sliderColor,
                start = Offset(sliderX, 0f),
                end = Offset(sliderX, size.height),
                strokeWidth = 4.dp.toPx()
            )
        }

        // 4. Draggable Slider Handle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Calculate new fractional position based on drag
                        val newPosition = sliderPosition + (dragAmount.x / maxWidthPx)
                        // Coerce between 0f (left edge) and 1f (right edge)
                        sliderPosition = newPosition.coerceIn(0f, 1f)
                    }
                }
        ) {
            Surface(
                modifier = Modifier
                    .offset { 
                        // Position the handle exactly over the line
                        IntOffset(
                            x = (maxWidthPx * sliderPosition - 20.dp.toPx()).roundToInt(),
                            y = (maxHeightPx / 2 - 20.dp.toPx()).roundToInt()
                        )
                    }
                    .size(40.dp),
                shape = CircleShape,
                color = sliderColor,
                shadowElevation = 8.dp
            ) {
                // Add a simple center dot or icon if desired
                Box(
                    contentAlignment = Alignment.Center, 
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }
            }
        }
        
        // 5. Labels
        Text(
            text = "BEFORE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Text(
            text = "AFTER",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * A custom Shape that only draws the left percentage of its bounds.
 * Useful for clipping the "Before" image based on the slider state.
 */
class FractionalWidthShape(private val widthFraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(
                left = 0f,
                top = 0f,
                right = size.width * widthFraction,
                bottom = size.height
            )
        )
    }
}
