package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.data.model.NewsArticle
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.PitchBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.PitchObsidian
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.SlateMuted
import com.example.ui.viewmodel.UiState

private val NEWS_FILTER_TAGS = listOf("All", "Premier League", "Champions League", "La Liga")

@Composable
fun NewsFeed(
    newsState: UiState<List<NewsArticle>>,
    onFetchNews: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTag by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        if (newsState !is UiState.Success) {
            onFetchNews()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PitchEmeraldContainer)
                            .border(1.dp, PitchEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Newspaper,
                            contentDescription = null,
                            tint = PitchEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Soccer News",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ChalkWhite
                        )
                        Text(
                            text = "Transfers, tactical analysis & headlines",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted
                        )
                    }
                }

                IconButton(
                    onClick = onFetchNews,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_news_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh news",
                        tint = PitchEmerald
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NEWS_FILTER_TAGS, key = { it }) { tag ->
                    val selected = tag == selectedTag
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PitchEmerald,
                            selectedLabelColor = PitchObsidian,
                            containerColor = PitchSurface,
                            labelColor = SlateMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = PitchBorder,
                            selectedBorderColor = PitchEmerald
                        ),
                        modifier = Modifier.testTag("news_filter_${tag.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (newsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PitchEmerald)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading latest soccer news...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorStatePanel(message = newsState.message, onRetry = onFetchNews)
                    }
                }

                is UiState.Success -> {
                    val filteredArticles = remember(newsState.data, selectedTag) {
                        if (selectedTag == "All") {
                            newsState.data
                        } else {
                            newsState.data.filter { it.leagueTag.equals(selectedTag, ignoreCase = true) }
                        }
                    }

                    if (filteredArticles.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStatePanel(
                                title = "No Headlines Available",
                                subtitle = "Switch filter or tap refresh to load recent football stories."
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("news_feed_list")
                        ) {
                            itemsIndexed(
                                items = filteredArticles,
                                key = { _, item -> item.id }
                            ) { index, article ->
                                if (index == 0) {
                                    FeaturedNewsArticleCard(
                                        article = article,
                                        onClick = { onArticleClick(article) }
                                    )
                                } else {
                                    CompactNewsArticleCard(
                                        article = article,
                                        fallbackRes = if (index % 2 == 0) {
                                            R.drawable.img_news_transfer
                                        } else {
                                            R.drawable.img_news_tactics
                                        },
                                        onClick = { onArticleClick(article) }
                                    )
                                }
                                if (index == 2 || (filteredArticles.size < 3 && index == filteredArticles.lastIndex)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AdMobBannerBar()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleDetailScreen(
    article: NewsArticle?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("news_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to news feed",
                        tint = ChalkWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = article?.leagueTag ?: "Article",
                    style = MaterialTheme.typography.titleLarge,
                    color = PitchEmerald
                )
            }

            if (article == null) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        title = "Article Unavailable",
                        subtitle = "Return to the news feed to pick another story."
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    NewsArticleImage(
                        imageUrl = article.imageUrl,
                        fallbackRes = R.drawable.img_news_tactics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${article.leagueTag.uppercase()} • ${article.publishedLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PitchEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChalkWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChalkWhite
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchEmerald,
                            contentColor = PitchObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("read_full_story_button")
                    ) {
                        Text(
                            text = "Read Full Story",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    AdMobBannerBar()
                }
            }
        }
    }
}

@Composable
private fun FeaturedNewsArticleCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, PitchBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("news_article_${article.id}")
    ) {
        NewsArticleImage(
            imageUrl = article.imageUrl,
            fallbackRes = R.drawable.img_news_tactics,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PitchObsidian.copy(alpha = 0.65f),
                            PitchObsidian.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PitchEmerald)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = article.leagueTag.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = PitchObsidian,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = ChalkWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${article.source} • ${article.publishedLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateMuted
            )
        }
    }
}

@Composable
private fun CompactNewsArticleCard(
    article: NewsArticle,
    fallbackRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurface)
            .border(1.dp, PitchBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("news_article_${article.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NewsArticleImage(
            imageUrl = article.imageUrl,
            fallbackRes = fallbackRes,
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.leagueTag.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = PitchEmerald,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = ChalkWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = article.publishedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateMuted
            )
        }
    }
}

@Composable
fun NewsArticleImage(
    imageUrl: String?,
    fallbackRes: Int,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                Image(
                    painter = painterResource(id = fallbackRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}
