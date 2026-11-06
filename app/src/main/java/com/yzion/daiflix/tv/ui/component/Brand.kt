package com.yzion.daiflix.tv.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.yzion.daiflix.tv.R
import com.yzion.daiflix.tv.ui.theme.DaiflixColors

/** The square "D" mark, sized by height. */
@Composable
fun DaiflixMark(
    modifier: Modifier = Modifier,
    height: Dp = 32.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_logo_d),
        contentDescription = null,
        modifier = modifier.height(height)
    )
}

/** Full brand lockup: D mark + DAIFLIX wordmark. */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    height: Dp = 34.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        DaiflixMark(height = height)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "DAIFLIX",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = DaiflixColors.Red
        )
    }
}

