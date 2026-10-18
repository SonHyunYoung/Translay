package com.example.transrator.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transrator.ui.theme.Pretendard

@Composable
fun ProfileEditScreen(onDone: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileHeader("프로필 편집", "편집/삭제할 프로필을 선택해주세요")
        if (s.loading) CircularProgressIndicator()
        s.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Pretendard, fontSize = 13.sp)
        }
        ProfileGrid(s, showAdd = false, onProfile = { vm.openEdit(it) }, onAdd = {})
        Spacer(Modifier.height(32.dp))
        BlueButton(
            "설정 완료", onDone,
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp)
        )
    }
    if (s.dialogOpen) ProfileDialog(s, vm)
}