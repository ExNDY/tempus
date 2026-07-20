package com.cappielloantonio.tempo.ui.auth

import android.os.Bundle
import android.security.KeyChain
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.navigation.BottomMenuConfig
import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName
import com.cappielloantonio.tempo.navigation.Screen
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.requireActivity
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import java.util.UUID

object LoginRouteScreen : Screen.DefaultScreen {

    override val screenName: String = defaultScreenName()

    override fun bottomMenuConfig(): BottomMenuConfig = BottomMenuConfig.Hidden

    @Composable
    override fun Content(navController: NavController, args: Bundle?) {
        val activity = LocalContext.current.requireActivity<MainActivity>()
        val viewModel = remember(activity) {
            ViewModelProvider(activity)[LoginViewModel::class.java]
        }

        LoginRouteContent(
            viewModel = viewModel,
            onServerSelected = { server ->
                viewModel.selectServer(server)
                activity.goFromLogin()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginRouteContent(
    viewModel: LoginViewModel,
    onServerSelected: (Server) -> Unit,
) {
    val servers by viewModel.getServerList().observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<Server?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    serverToEdit = null
                    showDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.login_add_server_content_description),
                )
            }
        },
    ) { padding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.login_no_servers_message))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(servers) { server ->
                    ServerItem(
                        server = server,
                        onClick = { onServerSelected(server) },
                        onLongClick = {
                            serverToEdit = server
                            showDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showDialog) {
        ServerEditorDialog(
            server = serverToEdit,
            onDismiss = { showDialog = false },
            onSave = { newServer ->
                viewModel.addServer(newServer)
                showDialog = false
            },
            onDelete = { server ->
                viewModel.deleteServer(server)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerItem(
    server: Server,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = server.serverName, style = MaterialTheme.typography.titleMedium)
                Text(text = server.address, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = server.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerEditorDialog(
    server: Server?,
    onDismiss: () -> Unit,
    onSave: (Server) -> Unit,
    onDelete: (Server) -> Unit,
) {
    val activity = LocalContext.current.requireActivity<MainActivity>()

    var name by remember(server) { mutableStateOf(server?.serverName ?: "") }
    var username by remember(server) { mutableStateOf(server?.username ?: "") }
    var password by remember(server) { mutableStateOf("") }
    var url by remember(server) { mutableStateOf(server?.address ?: "") }
    var localUrl by remember(server) { mutableStateOf(server?.localAddress ?: "") }
    var isLowSecurity by remember(server) { mutableStateOf(server?.isLowSecurity ?: false) }
    var clientCertAlias by remember(server) { mutableStateOf(server?.clientCert ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (server == null) {
                        R.string.server_signup_dialog_title
                    } else {
                        R.string.server_signup_dialog_title_edit
                    },
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_username)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                )
                OutlinedTextField(
                    value = localUrl,
                    onValueChange = { localUrl = it },
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_local_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("http://...") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isLowSecurity,
                        onCheckedChange = { isLowSecurity = it },
                    )
                    Text(stringResource(R.string.server_signup_dialog_action_low_security))
                }
                OutlinedTextField(
                    value = clientCertAlias,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.server_signup_dialog_hint_client_certificate)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            KeyChain.choosePrivateKeyAlias(
                                activity,
                                { alias -> clientCertAlias = alias ?: "" },
                                null,
                                null,
                                null,
                                null,
                            )
                        },
                    trailingIcon = {
                        if (clientCertAlias.isNotEmpty()) {
                            IconButton(onClick = { clientCertAlias = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(
                                        R.string.server_signup_dialog_clear_certificate
                                    ),
                                )
                            }
                        } else {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPassword = if (isLowSecurity) {
                        MusicUtil.passwordHexEncoding(password)
                    } else {
                        password
                    }
                    onSave(
                        Server(
                            serverId = server?.serverId ?: UUID.randomUUID().toString(),
                            serverName = name,
                            username = username,
                            password = finalPassword,
                            address = url,
                            localAddress = localUrl.ifBlank { null },
                            timestamp = System.currentTimeMillis(),
                            isLowSecurity = isLowSecurity,
                            clientCert = clientCertAlias.ifBlank { null },
                        ),
                    )
                },
            ) {
                Text(text = stringResource(R.string.server_signup_dialog_positive_button))
            }
        },
        dismissButton = {
            Row {
                if (server != null) {
                    TextButton(onClick = { onDelete(server) }) {
                        Text(
                            text = stringResource(R.string.server_signup_dialog_neutral_button),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.server_signup_dialog_negative_button))
                }
            }
        },
    )
}
