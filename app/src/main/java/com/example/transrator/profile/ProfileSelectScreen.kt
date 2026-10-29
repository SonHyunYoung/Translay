package com.example.transrator.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.ui.theme.Pretendard

@Composable
fun ProfileSelectScreen(
    onSelected: () -> Unit,
    onEdit: () -> Unit,
    vm: ProfileViewModel = viewModel()
) {
    val s by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileHeader("프로필 선택", "접속할 프로필을 선택해주십시오.")
        if (s.loading) CircularProgressIndicator()
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 17.sp)
        }
        ProfileGrid(s, showAdd = true, onProfile = { vm.select(it); onSelected() }, onAdd = { vm.openCreate() })
        Spacer(Modifier.height(40.dp))
        BlueButton(
            "프로필 편집", onEdit,
            Modifier.fillMaxWidth().padding(horizontal = 30.dp).height(56.dp)
        )
    }
    if (s.dialogOpen) ProfileDialog(s, vm)
}