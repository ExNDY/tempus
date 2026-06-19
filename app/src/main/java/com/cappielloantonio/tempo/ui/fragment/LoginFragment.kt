package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.security.KeyChain
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import androidx.media3.common.util.UnstableApi

@UnstableApi
class LoginFragment : Fragment() {

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TempusTheme {
                    LoginScreen(
                        viewModel = loginViewModel,
                        onServerSelected = { server ->
                            loginViewModel.selectServer(server)
                            (activity as? MainActivity)?.goFromLogin()
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen(
        viewModel: LoginViewModel,
        onServerSelected: (Server) -> Unit
    ) {
        val servers by viewModel.getServerList().observeAsState(emptyList<Server>())
        var showDialog by remember { mutableStateOf(false) }
        var serverToEdit by remember { mutableStateOf<Server?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.login_title)) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    serverToEdit = null
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Server")
                }
            }
        ) { padding ->
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No servers added yet. Click + to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        ServerItem(
                            server = server,
                            onClick = { onServerSelected(server) },
                            onLongClick = {
                                serverToEdit = server
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            ServerSignupDialog(
                server = serverToEdit,
                onDismiss = { showDialog = false },
                onSave = { newServer ->
                    viewModel.addServer(newServer)
                    showDialog = false
                },
                onDelete = { serverToDelete ->
                    viewModel.deleteServer(serverToDelete)
                    showDialog = false
                }
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ServerItem(
        server: Server,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = server.serverName, style = MaterialTheme.typography.titleMedium)
                    Text(text = server.address, style = MaterialTheme.typography.bodyMedium)
                    Text(text = server.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ServerSignupDialog(
        server: Server?,
        onDismiss: () -> Unit,
        onSave: (Server) -> Unit,
        onDelete: (Server) -> Unit
    ) {
        var name by remember { mutableStateOf(server?.serverName ?: "") }
        var username by remember { mutableStateOf(server?.username ?: "") }
        var password by remember { mutableStateOf("") }
        var url by remember { mutableStateOf(server?.address ?: "") }
        var localUrl by remember { mutableStateOf(server?.localAddress ?: "") }
        var isLowSecurity by remember { mutableStateOf(server?.isLowSecurity ?: false) }
        var clientCertAlias by remember { mutableStateOf(server?.clientCert ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = if (server == null) "Add Server" else "Edit Server") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Server Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...") }
                    )
                    OutlinedTextField(
                        value = localUrl,
                        onValueChange = { localUrl = it },
                        label = { Text("Local URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://...") }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = isLowSecurity, onCheckedChange = { isLowSecurity = it })
                        Text("Low Security (Hex Encoding)")
                    }
                    OutlinedTextField(
                        value = clientCertAlias,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Client Certificate") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                KeyChain.choosePrivateKeyAlias(requireActivity(), { alias ->
                                    clientCertAlias = alias ?: ""
                                }, null, null, null, null)
                            },
                        trailingIcon = {
                            if (clientCertAlias.isNotEmpty()) {
                                IconButton(onClick = { clientCertAlias = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalPassword = if (isLowSecurity) MusicUtil.passwordHexEncoding(password) else password
                    val newServer = Server(
                        server?.serverId ?: UUID.randomUUID().toString(),
                        name,
                        username,
                        finalPassword,
                        url,
                        localUrl.ifBlank { null },
                        System.currentTimeMillis(),
                        isLowSecurity,
                        clientCertAlias.ifBlank { null }
                    )
                    onSave(newServer)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (server != null) {
                        TextButton(onClick = { onDelete(server) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
