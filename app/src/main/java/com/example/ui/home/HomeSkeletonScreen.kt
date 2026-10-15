package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.shimmerEffect
import com.example.ui.theme.DarkBackground

/**
 * Skeleton loading screen displayed while the catalog is fetching or initializing.
 * Matches the layout hierarchy and dark aesthetic of the production catalog.
 */
@Composable
fun HomeSkeletonScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top App Bar Shimmer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )

                // Search field placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .shimmerEffect()
                )

                // Bell icon placeholder
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
            }
        }

        // Hero Spotlight Banner Shimmer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top tag placeholder
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )

                    Column {
                        // Title placeholder
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle / genres placeholder
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action button placeholder
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Genre Chips Shimmer
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(6) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index % 2 == 0) 64.dp else 84.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .shimmerEffect()
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Row 1: Section Header + Horizontal Cards Shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(160.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(4) {
                    Column(
                        modifier = Modifier.width(135.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Row 2: Section Header + Horizontal Cards Shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(140.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(4) {
                    Column(
                        modifier = Modifier.width(135.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
