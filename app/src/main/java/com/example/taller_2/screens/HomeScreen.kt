package com.example.taller_2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.taller_2.R
import com.example.taller_2.components.CustomActionButton

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CustomActionButton(
                text = stringResource(id = R.string.btn_open_camera),
                icon = Icons.Default.CameraAlt,
                iconDescription = stringResource(id = R.string.cd_camera_icon),
                onClick = onNavigateToCamera,
                modifier = Modifier.widthIn(max = 280.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomActionButton(
                text = stringResource(id = R.string.btn_open_map),
                icon = Icons.Default.Map,
                iconDescription = stringResource(id = R.string.cd_map_icon),
                onClick = onNavigateToMap,
                modifier = Modifier.widthIn(max = 280.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
