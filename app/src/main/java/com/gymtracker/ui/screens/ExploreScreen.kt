package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gymtracker.data.Exercise
import com.gymtracker.data.ExerciseRepository
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Body Parts", "Targets", "Equipment", "Search")
    
    // View state navigation within this screen to keep MainActivity clean
    var selectedCategoryType by remember { mutableStateOf<String?>(null) } // "bodyPart", "target", "equipment"
    var selectedCategoryValue by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        bottomBar = {
            if (selectedExercise == null && selectedCategoryValue == null) {
                NavBar(currentRoute = currentRoute, onNavigate = onNavigate)
            }
        },
        containerColor = AppBlack
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedExercise != null) {
                ExerciseDetailView(
                    exercise = selectedExercise!!,
                    onBack = { selectedExercise = null }
                )
            } else if (selectedCategoryValue != null) {
                ExerciseListView(
                    title = selectedCategoryValue!!,
                    exercises = when (selectedCategoryType) {
                        "bodyPart" -> ExerciseRepository.getByBodyPart(selectedCategoryValue!!)
                        "target" -> ExerciseRepository.getByTarget(selectedCategoryValue!!)
                        "equipment" -> ExerciseRepository.getByEquipment(selectedCategoryValue!!)
                        else -> emptyList()
                    },
                    onBack = { selectedCategoryValue = null },
                    onExerciseClick = { selectedExercise = it }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Explore Exercises",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                    )
                    
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = AppBlack,
                        contentColor = Acid,
                        edgePadding = 24.dp,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, color = if (selectedTab == index) Acid else Color.Gray) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedTab) {
                        0 -> CategoryList(ExerciseRepository.getBodyParts()) { 
                            selectedCategoryType = "bodyPart"
                            selectedCategoryValue = it 
                        }
                        1 -> CategoryList(ExerciseRepository.getTargets()) { 
                            selectedCategoryType = "target"
                            selectedCategoryValue = it 
                        }
                        2 -> CategoryList(ExerciseRepository.getEquipment()) { 
                            selectedCategoryType = "equipment"
                            selectedCategoryValue = it 
                        }
                        3 -> SearchView(onExerciseClick = { selectedExercise = it })
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryList(items: List<String>, onClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Dim)
                    .clickable { onClick(item) }
                    .padding(20.dp)
            ) {
                Text(
                    text = item,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(onExerciseClick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                if (it.length >= 2) {
                    results = ExerciseRepository.searchByName(it)
                } else {
                    results = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search exercises...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Acid,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Dim, unfocusedContainerColor = Dim
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results) { exercise ->
                ExerciseItem(exercise, onExerciseClick)
            }
        }
    }
}

@Composable
fun ExerciseListView(
    title: String, 
    exercises: List<Exercise>, 
    onBack: () -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exercises) { exercise ->
                ExerciseItem(exercise, onExerciseClick)
            }
        }
    }
}

@Composable
fun ExerciseItem(exercise: Exercise, onClick: (Exercise) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Dim)
            .clickable { onClick(exercise) }
            .padding(16.dp)
    ) {
        Text(
            text = exercise.name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${exercise.bodyPart.replaceFirstChar { it.uppercase() }} • ${exercise.equipment.replaceFirstChar { it.uppercase() }}",
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ExerciseDetailView(
    exercise: Exercise,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(exercise.id) {
        ImageRequest.Builder(context)
            .data("https://${ExerciseRepository.RAPID_API_HOST}/image?exerciseId=${exercise.id}&resolution=360")
            .addHeader("x-rapidapi-key", ExerciseRepository.RAPID_API_KEY)
            .addHeader("x-rapidapi-host", ExerciseRepository.RAPID_API_HOST)
            .crossfade(true)
            .build()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            AsyncImage(
                model = imageRequest,
                contentDescription = exercise.name,
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .background(AppBlack.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text(
                    text = exercise.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailChip("Target", exercise.target)
                    DetailChip("Body Part", exercise.bodyPart)
                    DetailChip("Equipment", exercise.equipment)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Secondary Muscles",
                    color = Acid,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.secondaryMuscles.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } },
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Instructions",
                    color = Acid,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                exercise.instructions.forEachIndexed { index, instruction ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(
                            text = "${index + 1}.",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = instruction,
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Dim)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(
            text = value.replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
