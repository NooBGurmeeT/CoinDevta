package com.gurmeet.coindevta.presentation.home

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gurmeet.coindevta.domain.model.Coin

@OptIn(ExperimentalMaterial3Api::class)
class HomeScreenUi {

    @Composable
    fun LayUi(
        state: HomeScreenState,
        livePrices: Map<String, Double>,
        onAction: (HomeAction) -> Unit
    ) {

        var pinCandidate by remember { mutableStateOf<Coin?>(null) }

        val favorites = state.coins.filter { it.isFavorite }
        val pinnedCoin = state.coins.find { it.isPinned }

        Scaffold(
            containerColor = Color(0xFFF2F4F7),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Crypto Market",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                )
            }
        ) { padding ->

            if (state.isFoldableExpanded) {

                // ================================
                // EXPANDED MODE (ROW LAYOUT)
                // ================================

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    // LEFT → COIN LIST
                    CoinListSection(
                        state = state,
                        livePrices = livePrices,
                        pinnedCoin = pinnedCoin,
                        onAction = onAction,
                        onLongHold = { pinCandidate = it },
                        modifier = Modifier.weight(1f)
                    )

                    // RIGHT → FAVORITES + PINNED
                    ExpandedRightSection(
                        favorites = favorites,
                        pinnedCoin = pinnedCoin,
                        livePrices = livePrices,
                        modifier = Modifier.weight(0.7f)
                    )
                }

            } else {

                // ================================
                // NORMAL MODE (COLUMN)
                // ================================

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    FavoriteSection(
                        favorites = favorites,
                        state = state,
                        livePrices = livePrices,
                        onAction = onAction
                    )

                    SearchBarSection(state, onAction)

                    CoinListSection(
                        state = state,
                        livePrices = livePrices,
                        pinnedCoin = pinnedCoin,
                        onAction = onAction,
                        onLongHold = { pinCandidate = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.isBottomSheetOpen) {
                SortBottomSheet(state, onAction)
            }

            if (pinCandidate != null) {
                PinConfirmationSheet(
                    coin = pinCandidate!!,
                    alreadyPinned = pinnedCoin,
                    onPin = {
                        onAction(HomeAction.PinCoin(pinCandidate!!.symbol))
                        pinCandidate = null
                    },
                    onUnPin = {
                        onAction(HomeAction.UnPinCoin)
                        pinCandidate = null
                    },
                    onDismiss = { pinCandidate = null }
                )
            }
        }
    }

    @Composable
    private fun ExpandedRightSection(
        favorites: List<Coin>,
        pinnedCoin: Coin?,
        livePrices: Map<String, Double>,
        modifier: Modifier = Modifier
    ) {

        Card(
            modifier = modifier
                .fillMaxHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                // ==========================
                // SCROLLABLE FAVORITES
                // ==========================

                if (favorites.isNotEmpty()) {

                    Text(
                        "Favorites",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {

                        favorites.forEach { coin ->

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column {
                                    Text(
                                        coin.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        coin.symbol.uppercase(),
                                        fontSize = 12.sp,
                                        color = Color(0xFF667085)
                                    )
                                }

                                Text(
                                    "$ ${livePrices[coin.symbol] ?: coin.price}"
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // ==========================
                // FIXED PINNED SECTION
                // ==========================

                if (pinnedCoin != null) {

                    Divider(
                        thickness = 1.dp,
                        color = Color(0xFFE6E8EB)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Pinned Coin",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {
                            Text(
                                pinnedCoin.name,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                pinnedCoin.symbol.uppercase(),
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }

                        Text(
                            "$ ${livePrices[pinnedCoin.symbol] ?: pinnedCoin.price}"
                        )
                    }
                }
            }
        }
    }
    // -----------------------------------
    // SEARCH BAR
    // -----------------------------------

    @Composable
    private fun SearchBarSection(
        state: HomeScreenState,
        onAction: (HomeAction) -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 🔍 SEARCH FIELD
                TextField(
                    value = state.searchQuery,
                    onValueChange = {
                        onAction(HomeAction.SearchChanged(it))
                    },
                    placeholder = { Text("Search coin...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    )
                )

                Spacer(Modifier.width(8.dp))

                // 🎛 FILTER ICON
                IconButton(
                    onClick = {
                        onAction(HomeAction.OpenFilter)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color(0xFF667085)
                    )
                }
            }
        }
    }
    // -----------------------------------
    // COIN LIST
    // -----------------------------------

    @Composable
    private fun CoinListSection(
        state: HomeScreenState,
        livePrices: Map<String, Double>,
        pinnedCoin: Coin?,
        onAction: (HomeAction) -> Unit,
        onLongHold: (Coin) -> Unit,
        modifier: Modifier = Modifier
    ) {

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {

            items(state.coins, key = { it.symbol }) { coin ->

                CoinItemCard(
                    coin = coin,
                    price = livePrices[coin.symbol] ?: coin.price,
                    isPinned = pinnedCoin?.symbol == coin.symbol,
                    onAction = onAction,
                    onLongHold = onLongHold
                )
            }
        }
    }

    // -----------------------------------
    // COIN ITEM
    // -----------------------------------

    @Composable
    private fun CoinItemCard(
        coin: Coin,
        price: Double,
        isPinned: Boolean,
        onAction: (HomeAction) -> Unit,
        onLongHold: (Coin) -> Unit
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onAction(HomeAction.CoinClick(coin.symbol))
                        },
                        onLongPress = {
                            onLongHold(coin)
                        }
                    )
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        onAction(HomeAction.ToggleFavorite(coin.symbol))
                    }
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint =
                            if (coin.isFavorite)
                                Color(0xFFEF4444)
                            else
                                Color(0xFFD0D5DD)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = coin.symbol.uppercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF667085)
                        )
                    )
                }

                if (isPinned) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Text(
                    text = "$ ${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    // -----------------------------------
    // FAVORITES
    // -----------------------------------

    @Composable
    private fun FavoriteSection(
        favorites: List<Coin>,
        state: HomeScreenState,
        livePrices: Map<String, Double>,
        onAction: (HomeAction) -> Unit
    ) {

        if (favorites.isEmpty()) return

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            Column(Modifier.padding(16.dp)) {

                Text(
                    "Favorites",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(Modifier.height(12.dp))

                val visible =
                    if (state.isExpandedFavorites)
                        favorites
                    else
                        favorites.take(3)

                visible.forEach { coin ->

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {
                            Text(coin.name, fontWeight = FontWeight.Medium)
                            Text(
                                coin.symbol.uppercase(),
                                fontSize = 12.sp,
                                color = Color(0xFF667085)
                            )
                        }

                        Text("$ ${livePrices[coin.symbol] ?: coin.price}")
                    }

                    Spacer(Modifier.height(12.dp))
                }

                if (favorites.size > 3) {
                    TextButton(
                        onClick = {
                            onAction(HomeAction.ToggleExpandFavorites)
                        }
                    ) {
                        Text(
                            if (state.isExpandedFavorites)
                                "Show Less"
                            else
                                "Show More"
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------
    // FILTER BOTTOM SHEET
    // -----------------------------------

    @Composable
    private fun SortBottomSheet(
        state: HomeScreenState,
        onAction: (HomeAction) -> Unit
    ) {

        ModalBottomSheet(
            onDismissRequest = { onAction(HomeAction.CloseFilter) },
            containerColor = Color.White
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

                Text(
                    "Sort By",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(Modifier.height(20.dp))

                SortItem("Market Cap ↑", state.selectedSort == SortType.MARKET_CAP_ASC) {
                    onAction(HomeAction.SortSelected(SortType.MARKET_CAP_ASC))
                }

                SortItem("Market Cap ↓", state.selectedSort == SortType.MARKET_CAP_DESC) {
                    onAction(HomeAction.SortSelected(SortType.MARKET_CAP_DESC))
                }

                SortItem("24h Change ↑", state.selectedSort == SortType.CHANGE_ASC) {
                    onAction(HomeAction.SortSelected(SortType.CHANGE_ASC))
                }

                SortItem("24h Change ↓", state.selectedSort == SortType.CHANGE_DESC) {
                    onAction(HomeAction.SortSelected(SortType.CHANGE_DESC))
                }

                SortItem("Name A → Z", state.selectedSort == SortType.NAME_ASC) {
                    onAction(HomeAction.SortSelected(SortType.NAME_ASC))
                }

                SortItem("Name Z → A", state.selectedSort == SortType.NAME_DESC) {
                    onAction(HomeAction.SortSelected(SortType.NAME_DESC))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun SortItem(
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) Color.Black else Color(0xFF667085)
            )

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF22C55E)
                )
            }
        }
    }

    // -----------------------------------
    // PIN CONFIRMATION BOTTOM SHEET
    // -----------------------------------

    @Composable
    private fun PinConfirmationSheet(
        coin: Coin,
        alreadyPinned: Coin?,
        onPin: () -> Unit,
        onUnPin: () -> Unit,
        onDismiss: () -> Unit
    ) {

        val isSameCoinPinned =
            alreadyPinned?.symbol == coin.symbol

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

                Text(
                    text =
                        if (isSameCoinPinned)
                            "Unpin ${coin.name}?"
                        else
                            "Pin ${coin.name}?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(Modifier.height(16.dp))

                if (isSameCoinPinned) {

                    Text(
                        "This coin will stop showing live price updates in notification.",
                        color = Color(0xFF667085)
                    )

                } else {

                    Text(
                        "This coin will show live price updates in notification.",
                        color = Color(0xFF667085)
                    )

                    if (alreadyPinned != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "⚠ ${alreadyPinned.name} will be unpinned.",
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (isSameCoinPinned) onUnPin() else onPin()
                        }
                    ) {
                        Text("Confirm")
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}