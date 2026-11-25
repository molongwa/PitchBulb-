package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.NewsArticle
import com.example.ui.components.NewsFeed
import com.example.ui.viewmodel.UiState

@Composable
fun NewsScreen(
    newsState: UiState<List<NewsArticle>>,
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NewsFeed(
        newsState = newsState,
        onFetchNews = onRefresh,
        onArticleClick = onArticleClick,
        modifier = modifier
    )
}
