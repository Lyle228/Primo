package com.example.primo2.screen

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.primo2.activity.*
import com.example.primo2.ui.theme.*
import com.google.accompanist.permissions.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kizitonwose.calendar.core.daysOfWeek
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.util.MarkerIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.*

val colorset = arrayOf(LightRed, LightPurple, LightSkyBlue, LightGreen, LightYellow,LightRed, LightPurple, LightSkyBlue, LightGreen, LightYellow)
var entireDatePlanName:String? = ""
@Composable
fun informationPlace(modifier: Modifier = Modifier)
{
    Text(text = "즐겨찾기 페이지", style = MaterialTheme.typography.h4)
}
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalNaverMapApi::class, ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class, ExperimentalGlideComposeApi::class
)
@Composable
fun MapScreen(
    navController: NavController,
    requestManager:RequestManager,
    datePlanName:String?,
    leaderUID: String?,
    onSearchButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
){
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    val courseList = remember { mutableStateListOf<String>() }
    val commentList = remember { mutableStateListOf<String>() }
    val amountList = remember { mutableStateListOf<Int>() }
    val database = Firebase.database.reference.child("DatePlan").child(leaderUID.toString())
    entireDatePlanName = datePlanName
    /*
    LaunchedEffect(true)
    {
        courseList.clear()
        commentList.clear()
        database.child(datePlanName!!).child("course").get().addOnSuccessListener {
            for(i in 0 until it.childrenCount)
            {
                courseList.add(it.child(i.toString()).value.toString())
                commentList.add("")
            }
        }.addOnFailureListener{
        }

        database.child(datePlanName!!).child("comments").get().addOnSuccessListener {
            for(i in 0 until it.childrenCount)
            {
                commentList[i.toInt()] = it.child(i.toString()).value.toString()
            }
        }.addOnFailureListener{
        }
    }*/

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(locationTrackingMode = LocationTrackingMode.Follow
                    ,maxZoom = 20.0, minZoom = 5.0),
        )
    }
    val mapUiSettings by remember {
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
            if(commentList.size != dataSnapshot.childrenCount.toInt()){
                commentList.clear()
                amountList.clear()
                for(i in 0 until dataSnapshot.childrenCount.toInt()) {
                    commentList.add("")
                    amountList.add(0)
                }
            }


        }
        override fun onCancelled(databaseError: DatabaseError) {
            //실패
        }
    }


    val commentListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            for(i in 0 until amountList.size){
                amountList[i] = 0
            }
            for(i in 0 until courseList.size) {
                commentList[i] = dataSnapshot.child(courseList[i]).value.toString()
                val cmt = commentList[i]
                if(commentList[i].isNotEmpty()) {
                    for (k in 0 until cmt.length) {
                        if (cmt[k].toString() == "원") {
                            var j = k - 1
                            if (j >= 0) {
                                while (cmt[j].code in 48..57) {
                                    j--
                                }
                            }
                            if (j + 1 != k) {
                                amountList[i] += cmt.substring(j + 1, k).toInt()
                            }
                        }
                    }
                }
            }
        }
        override fun onCancelled(databaseError: DatabaseError) {
            //실패
        }
    }
    var startDate by remember { mutableStateOf("") }
    LaunchedEffect(true){
        database.child(datePlanName!!).child("startDate").get().addOnSuccessListener {
            startDate = it.value.toString()
        }
    }
    database.child(datePlanName!!).child("course").addValueEventListener(courseListener)
    database.child(datePlanName).child("comments").addValueEventListener(commentListener)
    BottomSheetScaffold(
        topBar = {
              maptopbar(onSearchButtonClicked = onSearchButtonClicked,navController = navController, startDate,amountList)
        },
        scaffoldState = scaffoldState,
        sheetContent = {
            BottomSheetContent(scaffoldState,bottomNaviID,bottomNaviTitle,bottomNaviPaint,bottomNaviInfo,requestManager,showMapInfo,datePlanName,leaderUID,courseList,commentList,amountList,onBottomNaviSizeChange = { bottomNaviSize = it }, onShowMapInfo = { showMapInfo = it}, cameraPositionState)
        },
        sheetPeekHeight = bottomNaviSize,
        drawerElevation = 0.dp,
        sheetElevation = 0.dp,
    )
    {
        modifier.padding(it)

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
                    Log.e("이 곳의 경도 위도는?", "" + coord.latitude + "," + coord.longitude)
                }


            )
            {
                val courseCoordiList = ArrayList<LatLng>()
                for(i in 0 until courseList.size){
                    courseCoordiList.add(LatLng(placeListHashMap[courseList[i]]!!.latitude,placeListHashMap[courseList[i]]!!.longitude))
                }
//                if(courseCoordiList.size > 1) {
//                    PolylineOverlay(courseCoordiList.toList()
//                        ,pattern = arrayOf(10.dp,5.dp)
//                        ,width = 2.dp
//                        ,joinType = LineJoin.Round
//                        ,color = Color.Gray)
//                }

                val latlist:ArrayList<LatLng> = arrayListOf()
                for (i in 0 until courseList.size) {

                    latlist.add(LatLng(placeListHashMap[courseList[i]]!!.latitude, placeListHashMap[courseList[i]]!!.longitude))
                        if (latlist.size >= 2) {
                            PathOverlay(
                                coords = latlist,
                                width = 3.dp,
                                color = Color.White,
                                outlineColor = Color.Black
                            )
                        }

                        Marker(
                            icon = OverlayImage.fromResource(R.drawable.ic_baseline_circle_24),
                            iconTintColor = colorset[i],
                            width = 20.dp,
                            height = 20.dp,
                            anchor = Offset(0.45f,0.45f),
                            state = MarkerState(
                                position = LatLng(
                                    placeListHashMap[courseList[i]]!!.latitude,
                                    placeListHashMap[courseList[i]]!!.longitude
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
                }
                // Marker(state = rememberMarkerState(position = BOUNDS_1.northEast))

            }

            }
            Column {
                //ShowLocationPermission()
            }
        }
    }

@Composable
fun BottomSheetBeforeSlide(ID:String,
                           title: String,
                           courseList: SnapshotStateList<String>,
                           commentList: SnapshotStateList<String>,
                           onShowMapInfo: (Boolean) -> Unit,leaderUID: String?,
                           datePlanName: String?) { // 위로 스와이프 하기전에 보이는거
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
                    text = "인천 연수구 송도동 24-5",
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
                                commentList.add("")
                                val database = Firebase.database.reference
                                    .child("DatePlan")
                                    .child(leaderUID.toString())
                                database
                                    .child(datePlanName!!)
                                    .child("course")
                                    .setValue(courseList)

                                database
                                    .child(datePlanName!!)
                                    .child("comments")
                                    .setValue(commentList)
                            } else {
                                Toast
                                    .makeText(context, "이미 추가된 장소입니다.", Toast.LENGTH_SHORT)
                                    .show(); }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalNaverMapApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun BottomSheetContent(
    scaffoldState: BottomSheetScaffoldState,
    ID:String,
    title: String, paint:String, info:String,
    requestManager: RequestManager,
    showMapInfo:Boolean,
    datePlanName: String?,
    leaderUID: String?,
    courseList: SnapshotStateList<String>,
    commentList: SnapshotStateList<String>,
    amountList: SnapshotStateList<Int>,
    onBottomNaviSizeChange: (Dp) -> Unit,
    onShowMapInfo: (Boolean) -> Unit,
    cameraPositionState: CameraPositionState
) { // 스와이프 한후에 보이는 전체
    var isVisible by remember{ mutableStateOf(true) }
    val database = Firebase.database.reference.child("DatePlan").child(leaderUID.toString())
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        courseList.add(to.index,courseList.removeAt(from.index))
        commentList.add(to.index,commentList.removeAt(from.index))
        amountList.add(to.index,amountList.removeAt(from.index))
        isVisible = false
        database.child(datePlanName!!).child("course").setValue(courseList)
    })
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column {
        if(showMapInfo) {
            onBottomNaviSizeChange(65.dp)

            scaffoldState.bottomSheetState.apply {
                if (progress.to.name != "Expanded"  && isCollapsed) {
                    BottomSheetBeforeSlide(
                        ID,
                        title,
                        courseList,
                        commentList,
                        onShowMapInfo,
                        leaderUID,
                        datePlanName
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = Color.White
                ) {

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { /*TODO*/ }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(25.dp),
                                    tint = Color.Black
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { /*TODO*/ }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(25.dp),
                                    tint = Color.Black
                                )
                            }
                        }
                        GlideImage(
                            model = paint,
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(vertical = 16.dp, horizontal = 16.dp)
                                .aspectRatio(16f / 10f)
                                .clip(shape = RoundedCornerShape(10))
                                .fillMaxWidth()
                        )
                        {
                            it
                                .thumbnail(
                                    requestManager
                                        .asDrawable()
                                        .load(paint)
                                        // .signature(signature)
                                        .override(128)
                                )
                            // .signature(signature)
                        }

                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "센트럴 파크",
                            color = Color.Black,
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Row {
                            placetag("걷기 좋은")
                            placetag("공원")
                            placetag("전통")
                        }
                        Spacer(modifier = Modifier.padding(8.dp))
                        Row{
                            buttonwithicon(ic = Icons.Outlined.FavoriteBorder, description = "저장하기")
                            Spacer(modifier = Modifier.padding(12.dp))
                            buttonwithicon(ic = painterResource(id = R.drawable.ic_outline_comment_24), description = "리뷰보기")
                            Spacer(modifier = Modifier.padding(12.dp))
                            buttonwithicon(ic = painterResource(id = R.drawable.ic_outline_calendar_month_24), description = "일정추가")
                        }
                        Divider(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            color = moreLightGray,
                            thickness = 2.dp
                        )

                        Surface( //지도
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black,
                            modifier = Modifier
                                .aspectRatio(16f / 10f)
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "지 도",
                                fontSize = 100.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            information(title = "주소", text = "인천 연수구 컨벤시아대로 160")
                            information("이용 가능 시간", "연중 무휴")
                        }
                        Divider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth(),
                            color = moreLightGray,
                            thickness = 2.dp
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "리뷰",
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            reviewform(name = "정석준", score = 4, text = "산책하기 정말 좋은 공원이에요")
                            reviewform(name = "삼삼삼", score = 4, text = "삼삼삼삼삼삼삼삼삼")
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(15.dp))
            //Text(text = info, fontFamily = FontFamily.Cursive)
        }
        else{
            Column {
                onBottomNaviSizeChange(350.dp)

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
                        .clickable { reorderBest(courseList, commentList,amountList) }
                ) {
                    Text(text = "거리순 정렬", color = Color.Black, modifier = Modifier.padding(8.dp))
                }
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - 187.dp)
                        .reorderable(state)
                ) {
                    items(courseList, { it }) { item ->
                        ReorderableItem(state, key = item) { isDragging ->
                            val placeName: String = placeListHashMap[item]?.placeName.toString()
                            val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                            var expanded by remember { mutableStateOf(false) }
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .shadow(
                                        elevation = 1.dp,
                                        shape = RoundedCornerShape(20)
                                    )
                            ) {
                                Column (
                                    modifier = Modifier
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        ) {
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .size(35.dp)
                                                .aspectRatio(1f)
                                                .background(
                                                    color = colorset[courseList.indexOf(item)],
                                                    shape = CircleShape,
                                                )
                                        ) {
                                            Text(
                                                text = (courseList.indexOf(item) + 1).toString(),
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
                                                    fontSize = 16.sp,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.clickable {
                                                        cameraPositionState.move(
                                                            CameraUpdate.scrollTo(
                                                                LatLng(
                                                                    placeListHashMap[item]!!.latitude,
                                                                    placeListHashMap[item]!!.longitude
                                                                )
                                                            )
                                                        )
                                                    }
                                                )
                                                Spacer(modifier = Modifier.padding(4.dp))
                                                Row {
                                                    for(i in 0 until placeListHashMap[item]!!.toptag.size - 2 ) {
                                                        placetag(placeListHashMap[item]!!.toptag[i], 10.sp)
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { expanded = !expanded }
                                                ) {
                                                    if(commentList[courseList.indexOf(item)] == "null")
                                                    {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_outline_description_24),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(20.dp),
                                                            tint = Color.Gray
                                                        )
                                                    }
                                                    else{
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_outline_description_24),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(20.dp),
                                                            tint = Color.Black
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Menu,
                                                    contentDescription = null,
                                                    modifier = Modifier.detectReorder(state)
                                                )
                                            }
                                        }
                                    }
                                    if (expanded) {
                                        Memoform(item,courseList, commentList,datePlanName)
                                    }
                                    Text(
                                        text = "예상 비용 : " + amountList[courseList.indexOf(item)] + "원",
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                }
                            }
                        }

                        if (courseList.indexOf(item) != courseList.size - 1) {
                            var distance = getDistance(
                                placeListHashMap[item]!!.latitude,
                                placeListHashMap[item]!!.longitude,
                                placeListHashMap[courseList[courseList.indexOf(item) + 1]]!!.latitude,
                                placeListHashMap[courseList[courseList.indexOf(item) + 1]]!!.longitude
                            ).toString()
                            var distanceUnit = "m"
                            if (distance.toDouble() >= 1000) {
                                distance = (round(distance.toDouble() / 100) / 10).toString()
                                distanceUnit = "km"
                            }
                            LaunchedEffect(isVisible)
                            {
                                delay(700L)
                                isVisible = true
                            }
                            Surface(modifier = Modifier.height(20.dp)) {
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
}

@Composable
fun maptopbar(onSearchButtonClicked: () -> Unit = {},navController: NavController,startDate:String,amountList: SnapshotStateList<Int>) {

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .clickable { navController.navigateUp() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .size(25.dp),
                        tint = Color.Black
                    )
                }
                Text(
                    text = "다음 데이트",
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    fontFamily = spoqasans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .clickable { onSearchButtonClicked() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(25.dp),
                        tint = Color.Black
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
            ) {
                if(startDate.isNotBlank()) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val printformat = DateTimeFormatter.ofPattern("MM월 dd일")

                    val weatherformatter = DateTimeFormatter.ofPattern("yyyyMMdd")

                    val Datedate = LocalDate.parse(startDate, formatter)
                    val dayOfWeekName = doDayOfWeek(Datedate)
                    var number = -1
                    for(i in 0 until weatherInfo.dateList.size)
                    {
                        if(LocalDate.parse(weatherInfo.dateList[i],weatherformatter).isEqual(Datedate) && weatherInfo.timeList[i] == "1200")
                        {
                            number = i
                            break
                        }
                    }
                    if(number == -1) {
                        Text(
                            text = Datedate.format(printformat) + " " + dayOfWeekName,
                            textAlign = TextAlign.Center,
                            color = Color.Black,
                            fontFamily = spoqasans,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    else{
                        Row()
                        {
                            Text(
                                text = Datedate.format(printformat) + " " + dayOfWeekName,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                fontFamily = spoqasans,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                            if(weatherInfo.typeList[number] == 0) // 비 x
                            {
                                if(weatherInfo.skyList[number] == 1) // 맑음
                                {
                                    Image(
                                        painter = painterResource(id = R.drawable.sunny),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(20.dp)
                                    )
                                }
                                else if(weatherInfo.skyList[number] == 3) // 구름많음
                                {
                                    Image(
                                        painter = painterResource(id = R.drawable.cloudy),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(20.dp)
                                    )
                                }
                                else if(weatherInfo.skyList[number] == 4) // 흐림
                                {
                                    Image(
                                        painter = painterResource(id = R.drawable.cloudy),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(20.dp)
                                    )
                                }

                            }

                            else if(weatherInfo.typeList[number] == 1)//비
                            {
                                Image(
                                    painter = painterResource(id = R.drawable.rainny),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                            else if(weatherInfo.typeList[number] == 2)// 비/눈
                            {
                                Image(
                                    painter = painterResource(id = R.drawable.rainny),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                            else if(weatherInfo.typeList[number] == 3)//눈
                            {
                                Image(
                                    painter = painterResource(id = R.drawable.snow),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                            else if(weatherInfo.typeList[number] == 4)//소나기
                            {
                                Image(
                                    painter = painterResource(id = R.drawable.rainny),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { /*TODO*/ },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    var totalAmount = 0
                    for(i in 0 until amountList.size)
                    {
                        totalAmount += amountList[i]
                    }
                    Text(
                        text = "예상 비용 : " + totalAmount.toString() + "원",
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontFamily = spoqasans,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.padding(4.dp))
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



@Composable
fun Memoform(courseName:String,courseList:SnapshotStateList<String>,commentList:SnapshotStateList<String>,datePlanName: String?) {
    Column (
        modifier = Modifier
    ) {
        val msg = if(commentList[courseList.indexOf(courseName)] == "null") "" else commentList[courseList.indexOf(courseName)]
        var content by remember { mutableStateOf(msg) }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = content,
            onValueChange = {
                commentList[courseList.indexOf(courseName)] = it
                content = it
                            },
            placeholder = {
                Text(
                    modifier = Modifier
                        .alpha(ContentAlpha.medium),
                    text = "메모를 추가해주세요",
                    color = Color.Gray
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.White,
                cursorColor = Color.Black,
                focusedIndicatorColor = White,
                unfocusedIndicatorColor = White
            ),
            shape = RoundedCornerShape(20.dp),
            maxLines = 10
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
            val database = Firebase.database.reference.child("DatePlan").child(leaderUID.toString())
            database
                .child(datePlanName!!)
                .child("comments")
                .child(courseName)
                .setValue(commentList[courseList.indexOf(courseName)])
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(width = 140.dp, height = 35.dp)
        ) {
            Text(text = "저장하기")
        }
    }
    Spacer(modifier = Modifier.size(8.dp))
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

fun reorderBest(courseList: SnapshotStateList<String>,commentList: SnapshotStateList<String>,amountList: SnapshotStateList<Int>)
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
        commentList.add(i+1,commentList.removeAt(bestIndex))
        amountList.add(i+1,amountList.removeAt(bestIndex))
    }
}

private fun doDayOfWeek(date:LocalDate): String? {
    var strWeek: String? = null
    val nWeek = date.dayOfWeek

    if (nWeek == DayOfWeek.SUNDAY) {
        strWeek = "일"
    } else if (nWeek == DayOfWeek.MONDAY) {
        strWeek = "월"
    } else if (nWeek == DayOfWeek.TUESDAY) {
        strWeek = "화"
    } else if (nWeek == DayOfWeek.WEDNESDAY) {
        strWeek = "수"
    } else if (nWeek == DayOfWeek.THURSDAY) {
        strWeek = "목"
    } else if (nWeek == DayOfWeek.FRIDAY) {
        strWeek = "금"
    } else if (nWeek == DayOfWeek.SATURDAY) {
        strWeek = "토"
    }
    return strWeek
}