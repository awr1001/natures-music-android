package com.aaronroberts.naturesmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronroberts.naturesmusic.ui.theme.AppGray
import com.aaronroberts.naturesmusic.ui.theme.ButtonFill
import com.aaronroberts.naturesmusic.ui.theme.Hairline
import com.aaronroberts.naturesmusic.ui.theme.Ink
import com.aaronroberts.naturesmusic.ui.theme.YouTubeRed

private val PillShape = RoundedCornerShape(50)

@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 28.dp),
    useColorBackground: Boolean = true,
    titleColor: Color = Ink,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (useColorBackground) Modifier.background(AppGray) else Modifier)
            .padding(contentPadding),
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp),
        )
        content()
    }
}

@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    labelColor: Color = Ink,
    backgroundColor: Color = ButtonFill,
    borderColor: Color = Hairline,
    cornerRadius: Dp = 50.dp,
    fillMaxWidth: Boolean = true,
    fontSize: TextUnit = 18.sp,
    maxLines: Int = 2,
) {
    val shape = if (cornerRadius >= 50.dp) PillShape else RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** iOS-matching red ▶︎ pill that opens the demonstration video. */
@Composable
fun YouTubeLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PillButton(
        label = "▶︎",
        onClick = onClick,
        modifier = modifier.width(150.dp),
        height = 41.dp,
        fillMaxWidth = false,
        labelColor = Color.White,
        backgroundColor = YouTubeRed,
        borderColor = Hairline,
        cornerRadius = 15.dp,
        fontSize = 22.sp,
        maxLines = 1,
    )
}

@Composable
fun CircleGlyphButton(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ButtonFill)
            .border(1.5.dp, Hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
