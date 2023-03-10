package com.example.primo2.screen

import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.primo2.*
import com.example.primo2.R
import com.example.primo2.ui.theme.*
import com.google.accompanist.permissions.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PolylineOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.*

val colorset = arrayOf(LightRed, LightPurple, LightSkyBlue, LightGreen, LightYellow)

@Composable
fun informationPlace(modifier: Modifier = Modifier)
{
    Text(text = "???????????? ?????????", style = MaterialTheme.typography.h4)
}
@OptIn(ExperimentalNaverMapApi::class, ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class, ExperimentalGlideComposeApi::class
)
@Composable
fun MapScreen(
    navController: NavController,
    requestManager:RequestManager,
    datePlanName:String?,
    leaderUID: String?,
    modifier: Modifier = Modifier
){
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    val courseList = remember { mutableStateListOf<String>() }
    val database = Firebase.database.reference.child("DatePlan").child(leaderUID.toString())
    courseList.clear()
    database.child(datePlanName!!).child("course").get().addOnSuccessListener {
        for(i in 0 until it.childrenCount)
        {
            courseList.add(it.child(i.toString()).value.toString())
        }
    }.addOnFailureListener{
    }

    var mapProperties by remember {
        mutableStateOf(
            MapProperties(locationTrackingMode = LocationTrackingMode.Follow
                    ,maxZoom = 20.0, minZoom = 5.0),
        )
    }
    var mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(isLocationButtonEnabled = true, isIndoorLevelPickerEnabled = true)
        )
    }
    val cameraPositionState = rememberCameraPositionState()
    val position by remember {
        derivedStateOf {
            cameraPositionState.position
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    var bottomNaviSize by remember { mutableStateOf(65.dp) }
    var bottomNaviTitle by remember { mutableStateOf("") }
    var bottomNaviID by remember { mutableStateOf("") }
    var bottomNaviInfo by remember { mutableStateOf("") }
    var bottomNaviPaint by remember { mutableStateOf("") }

    var showMapInfo by remember { mutableStateOf(false) }

    val courseListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            courseList.clear()
            for (i in 0 until dataSnapshot.childrenCount) {
                courseList.add(
                    dataSnapshot.child(i.toString()).value.toString()
                )
            }
        }
        override fun onCancelled(databaseError: DatabaseError) {
            //??????
        }
    }
    database!!.child(datePlanName!!).child("course").addValueEventListener(courseListener)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            BottomSheetContent(bottomNaviID,bottomNaviTitle,bottomNaviPaint,bottomNaviInfo,requestManager,showMapInfo,datePlanName,leaderUID,courseList,onBottomNaviSizeChange = { bottomNaviSize = it }, onShowMapInfo = { showMapInfo = it}, cameraPositionState)
        },
        sheetPeekHeight = bottomNaviSize,
    )
    {
        modifier.padding(it)
        var searchKeyword by remember { mutableStateOf("") }
        Box(
            Modifier
                .height(screenHeight - bottomNaviSize)) {

            NaverMap(cameraPositionState = cameraPositionState,
                locationSource = rememberFusedLocationSource(),
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = { _, coord ->
                    scope.launch {
                            scaffoldState.bottomSheetState.apply{
                                if (!isCollapsed) {collapse()}
                        }
                    }
                    showMapInfo = false
                    Log.e("??? ?????? ?????? ??????????", "" + coord.latitude + "," + coord.longitude)
                }


            )
            {
                val courseCoordiList = ArrayList<LatLng>()
                for(i in 0 until courseList.size){
                    courseCoordiList.add(LatLng(placeListHashMap[courseList[i]]!!.latitude,placeListHashMap[courseList[i]]!!.longitude))
                }
                if(courseCoordiList.size > 1) {
                    PolylineOverlay(courseCoordiList.toList()
                        ,pattern = arrayOf(10.dp,5.dp)
                        ,width = 2.dp
                        ,joinType = LineJoin.Round
                        ,color = Color.Gray)
                }

                for (i in 0 until placeList.size) {
                    val courseIndex = courseList.indexOf(placeList[i].placeID)
                    if (courseIndex != -1) {
                        Marker(
                            icon = OverlayImage.fromResource(R.drawable.circle),
                            width = 20.dp,
                            height = 20.dp,
                            state = MarkerState(
                                position = LatLng(
                                    placeList[i].latitude,
                                    placeList[i].longitude
                                )
                            ),
                            //captionText = placeList[i].placeName + "\n" + (courseIndex + 1),
                            //captionColor = Color.Green,
                            onClick = { overlay ->
                                bottomNaviInfo = placeList[i].information
                                bottomNaviID = placeList[i].placeID
                                bottomNaviTitle = placeList[i].placeName
                                bottomNaviPaint = placeList[i].imageResource
                                showMapInfo = true

                                scope.launch {
                                    scaffoldState.bottomSheetState.apply {
                                        if (!isCollapsed) {
                                            collapse()
                                        }
                                    }
                                }

                                true
                            },
                            tag = i,
                        )
                    } else {
//                        var fitness: Double = fitnessCalc(userOrientation, i)
//                        Marker(
//                            icon = OverlayImage.fromResource(R.drawable.ic_baseline_place_24),
//                            width = 40.dp,
//                            height = 40.dp,
//                            state = MarkerState(
//                                position = LatLng(
//                                    placeList[i].latitude,
//                                    placeList[i].longitude
//                                )
//                            ),
//                            captionText = placeList[i].placeName + "\n" + "????????? : " + fitness.roundToInt() + "%",
//                            captionMinZoom = 12.2,
//                            minZoom = 12.2,
//                            onClick = { overlay ->
//                                bottomNaviInfo = placeList[i].information
//                                bottomNaviID = placeList[i].placeID
//                                bottomNaviTitle = placeList[i].placeName
//                                bottomNaviPaint = placeList[i].imageResource
//                                showMapInfo = true
//
//                                scope.launch {
//                                    scaffoldState.bottomSheetState.apply {
//                                        if (!isCollapsed) {
//                                            collapse()
//                                        }
//                                    }
//                                }
//
//                                true
//                            },
//                            tag = i,
//                            zIndex = fitness.roundToInt() // ????????? ????????? ????????? ?????? ??????
//                        )
                    }
                }
                // Marker(state = rememberMarkerState(position = BOUNDS_1.northEast))
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.White)
                    .shadow(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { /*TODO*/ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp),
                            tint = Color.Black
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "?????? ?????????",
                            textAlign = TextAlign.Center,
                            color = Color.Black,
                            fontFamily = spoqasans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "3??? 23??? ???",
                            textAlign = TextAlign.Center,
                            color = Color.Black,
                            fontFamily = spoqasans,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { /*TODO*/ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
            //?????????

//            Column(modifier=Modifier) {
//                Box(modifier = Modifier) {
//                    TextField(
//                        value = searchKeyword,
//                        onValueChange = { text ->
//                            searchKeyword = text
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(50.dp),
//                        textStyle = TextStyle.Default.copy(fontSize = 10.sp,),
//                        label = { Text("??????") },
//                        singleLine = true,
//                        colors = TextFieldDefaults.textFieldColors(
//                            backgroundColor = Color.White,
//                            cursorColor = Color.Black,
//                            focusedIndicatorColor = Color.Black,
//                            focusedLabelColor = Color.Black
//                        )
//                    )
//                }
//            }
//            Column(modifier = Modifier.padding(10.dp)) {
//                Box(modifier = Modifier) {
//                    TextField(
//                        value = searchKeyword,
//                        onValueChange = { text ->
//                            searchKeyword = text
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(50.dp),
//                        textStyle = TextStyle.Default.copy(fontSize = 10.sp,),
//                        label = { Text("??????") },
//                        singleLine = true,
//                        colors = TextFieldDefaults.textFieldColors(
//                            backgroundColor = Color.White,
//                            cursorColor = Color.Black,
//                            focusedIndicatorColor = Color.Black,
//                            focusedLabelColor = Color.Black
//                        )
//                    )
//                }
                val searchPlaceList:ArrayList<Int> = ArrayList()
                if(searchKeyword.isNotBlank()){
                    searchPlaceList.clear()
                    for(i in 0 until placeList.size){
                        if(placeList[i].placeName.contains(searchKeyword)){
                            searchPlaceList.add(i)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    items(searchPlaceList) { item ->
                        Column(modifier = Modifier
                            .height(50.dp)
                            .fillMaxWidth()
                            .background(Color.White)) {
                            Row(modifier = Modifier) {
                                val url = placeList[item].imageResource
                                GlideImage(
                                    model = url, contentDescription = "", modifier = Modifier
                                        .height(40.dp)
                                        .width(40.dp), contentScale = ContentScale.Crop

                                )
                                {
                                    it
                                        .thumbnail(
                                            requestManager
                                                .asDrawable()
                                                .load(url)
                                                // .signature(signature)
                                                .override(64)
                                        )
                                    // .signature(signature)
                                }
                                Text(text = placeList[item].placeName)
                            }
                        }
                    }
                }
            }
            Column {
                //ShowLocationPermission()
            }
        }
    }

@Composable
fun BottomSheetBeforeSlide(ID:String, title: String,courseList: SnapshotStateList<String>,onShowMapInfo: (Boolean) -> Unit,leaderUID: String?,datePlanName: String?) { // ?????? ???????????? ???????????? ????????????
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .height(65.dp)
            .background(color = Color.White)
            .padding(start = 0.dp, top = 5.dp), verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(0.dp))
            Column(modifier = Modifier, verticalArrangement = Arrangement.Top) {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Cursive
                )
                Text(
                    text = "?????? ????????? ????????? 24-5",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Cursive
                )
            }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .height(65.dp)
                .background(color = Color.White)
                .padding(start = 0.dp, top = 5.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.End

        ) {

            Row(modifier = Modifier, horizontalArrangement = Arrangement.End) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable {
                            onShowMapInfo(false)
                            if (courseList.indexOf(ID) == -1) {
                                courseList.add(ID)
                                val database = Firebase.database.reference
                                    .child("DatePlan")
                                    .child(leaderUID.toString())
                                database
                                    .child(datePlanName!!)
                                    .child("course")
                                    .setValue(courseList)
                            } else {
                                Toast
                                    .makeText(context, "?????? ????????? ???????????????.", Toast.LENGTH_SHORT)
                                    .show(); }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalNaverMapApi::class)
@Composable
fun BottomSheetContent(
    ID:String,
    title: String, paint:String, info:String,
    requestManager: RequestManager,
    showMapInfo:Boolean,
    datePlanName: String?,
    leaderUID: String?,
    courseList: SnapshotStateList<String>,
    onBottomNaviSizeChange: (Dp) -> Unit,
    onShowMapInfo: (Boolean) -> Unit,
    cameraPositionState: CameraPositionState
) { // ???????????? ????????? ????????? ??????
    var isVisible by remember{ mutableStateOf(true) }
    val database = Firebase.database.reference.child("DatePlan").child(leaderUID.toString())
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        courseList.add(to.index,courseList.removeAt(from.index))
        isVisible = false
        database.child(datePlanName!!).child("course").setValue(courseList)
    })
    Column {
        if(showMapInfo) {
            onBottomNaviSizeChange(65.dp)
            BottomSheetBeforeSlide(ID,title,courseList, onShowMapInfo,leaderUID,datePlanName)
            GlideImage(
                model = paint, contentDescription = "", modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth(), contentScale = ContentScale.Crop
            )
            {
                it
                    .thumbnail(
                        requestManager
                            .asDrawable()
                            .load(paint)
                            // .signature(signature)
                            .override(64)
                    )
                // .signature(signature)
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(text = info, fontFamily = FontFamily.Cursive)
        }
        else{
            onBottomNaviSizeChange(400.dp)

            Spacer(modifier = Modifier.padding(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 2.dp)
                        .align(Alignment.Center)
                        .background(color = Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.padding(4.dp))
            Box(
                modifier = Modifier
                    .padding(vertical = 0.dp, horizontal = 16.dp)
                    .align(Alignment.Start)
                    //.border(1.dp,Color.LightGray, RoundedCornerShape(20))
                    .background(
                        color = moreLightGray,
                        shape = RoundedCornerShape(20),
                    )
                    .clip(RoundedCornerShape(20))
                    .clickable { reorderBest(courseList) }
            ) {
                Text(text = "????????? ??????",color= Color.Black, modifier = Modifier.padding(8.dp))
            }
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .reorderable(state)
            ) {
                items(courseList, { it }) { item ->
                    ReorderableItem(state, key = item) { isDragging ->
                        val placeName: String = placeListHashMap[item]?.placeName.toString()
                        val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .shadow(
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(20)
                                )
                                .aspectRatio(20f / 4.5f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .size(40.dp)
                                        .aspectRatio(1f)
                                        .background(
                                            color = colorset[courseList.indexOf(item)],
                                            shape = CircleShape,
                                        )
                                ) {
                                    Text(
                                        text = (courseList.indexOf(item)+1).toString(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                    )
                                }
                                Divider(
                                    color = moreLightGray,
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(60.dp),
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                    ) {
                                        Text(
                                            text = placeName,
                                            color = Color.Black,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable{
                                                cameraPositionState.move(CameraUpdate.scrollTo(
                                                    LatLng(placeListHashMap[item]!!.latitude,
                                                        placeListHashMap[item]!!.longitude)
                                                ))
                                            }
                                        )
                                        Spacer(modifier = Modifier.padding(4.dp))
                                        Row {
                                            placetag("?????? ??????")
                                            placetag("??????")
                                            placetag("??????")
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        modifier = Modifier.detectReorder(state)
                                    )
                                }
                            }
                        }
                    }

                    if(courseList.indexOf(item) != courseList.size - 1 )
                    {
                        var distance = getDistance(placeListHashMap[item]!!.latitude,
                            placeListHashMap[item]!!.longitude,
                        placeListHashMap[courseList[courseList.indexOf(item)+1]]!!.latitude,
                            placeListHashMap[courseList[courseList.indexOf(item)+1]]!!.longitude).toString()
                        var distanceUnit = "m"
                        if(distance.toDouble() >= 1000)
                        {
                            distance = (round(distance.toDouble() / 100) /10).toString()
                            distanceUnit = "km"
                        }
                        LaunchedEffect(isVisible)
                        {
                            delay(700L)
                            isVisible = true
                        }
                        Surface (modifier = Modifier.height(20.dp)){
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(16.dp),
                                        tint = Color.Black
                                    )
                                    Text(
                                        text = "$distance$distanceUnit",
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier
                                    )
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
fun placetag(tagname : String){
    Box(
        modifier = Modifier
            .border(
                1.dp,
                Color.LightGray,
                RoundedCornerShape(60)
            )
    ) {
        Text(
            text = tagname,
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(6.dp)
        )
    }
    Spacer(modifier = Modifier.padding(4.dp))
}


fun fitnessCalc(userOrientation: HashMap<String, Any>,num :Int) : Double{
    var fitness:Double = 0.0
    for((key, value) in userOrientation){
        if(placeList[num].placeHashMap?.containsKey(key) == true)
        {
            val tmp = placeList[num].placeHashMap?.get(key).toString().toDouble()
            fitness += tmp * value.toString().toDouble() * 20
        }
    }

    //fitness += 20 + ((userOrientation["IE"]!! * placeList[num].static) - (userOrientation["IE"]!! * placeList[num].active))*10
    //fitness += 20 +((userOrientation["NS"]!! * placeList[num].nature) - (userOrientation["NS"]!! * placeList[num].city))*10
    //fitness += 20 +((userOrientation["FT"]!! * placeList[num].focusFood) - (userOrientation["FT"]!! * placeList[num].focusTour))*10
   //fitness += 20 +((userOrientation["PJ"]!! * placeList[num].lazy) - (userOrientation["PJ"]!! * placeList[num].faithful))*10

    if(fitness > 100)
    {
        fitness = 100.0
    }
    return fitness
}

fun getDistance(lat1: Double, long1: Double,lat2:Double, long2:Double) : Int{
    val R = 6372.8 * 1000
    val dLat = Math.toRadians(lat2- lat1)
    val dLong = Math.toRadians(long2 - long1)
    val a = sin(dLat/2).pow(2.0) + sin(dLong / 2).pow(2.0) * cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
    val c = 2 * asin(sqrt(a))
    return round(R * c).toInt()
}

fun reorderBest(courseList: SnapshotStateList<String>)
{
    for(i in 0 until courseList.size-1)
    {
        var bestIndex = -1
        var bestDistance = 100000
        for(j in i+1 until courseList.size )
        {
            val tryDistance = getDistance(placeListHashMap[courseList[i]]!!.latitude, placeListHashMap[courseList[i]]!!.longitude,
                placeListHashMap[courseList[j]]!!.latitude, placeListHashMap[courseList[j]]!!.longitude)
            if(tryDistance < bestDistance)
            {
                bestDistance = tryDistance
                bestIndex = j
            }
        }
        courseList.add(i+1,courseList.removeAt(bestIndex))
    }
}
/*
@Preview(showBackground = true)
@Composable
fun BottomSheetListItemPreview() {
    BottomSheetBeforeSlide(ID = "CentrolPark",title = "????????? ??????")
}
 */

/*
@Preview(showBackground = true)
@Composable
fun BottomSheetContentPreview(requestManager:RequestManager) {
    BottomSheetContent("???????????????","https://firebasestorage.googleapis.com/v0/b/primo-92b68.appspot.com/o/places%2F%EC%86%94%EC%B0%AC%EA%B3%B5%EC%9B%90.jpg?alt=media&token=cb9ace94-0d86-4cf1-8065-b6781b8ea30d","????????? ???????????? ????????? ?????????!",requestManager)
}

 */



/*
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ShowLocationPermission(){

    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf( android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION)
    )

    if (cameraPermissionState.allPermissionsGranted) {
        Text("???????????? ????????? ?????????")
    } else {
        val textToShow = if (cameraPermissionState.shouldShowRationale) {
            // ???????????? ?????? ????????? ??????????????? ????????? ????????? ??? ?????? ??????, ?????? ??? ????????? ????????? ????????? ???????????? ???????????????.
            "?????? ????????? ???????????? ????????? ??????????????? ??????????????????."
        } else {
            // ???????????? ??? ????????? ?????? ???????????????, ??????????????? ??? ????????? ?????? ?????? ?????? ?????? ?????? ????????? ??????????????? ???????????????.
            "?????? ????????? ??????????????? ??????????????? ???????????????." +
                    "????????? ??????????????????."
        }
        Text(textToShow)
        Button(onClick = { cameraPermissionState.launchMultiplePermissionRequest() }) {
            Text("?????? ??????")
        }
    }
}

*/






