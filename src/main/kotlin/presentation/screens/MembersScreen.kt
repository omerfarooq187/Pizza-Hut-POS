package presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.model.Member
import org.koin.compose.koinInject
import presentation.viewmodel.MemberViewModel

@Composable
fun MemberScreen(viewModel: MemberViewModel = koinInject()) {
    val members = viewModel.members
    val error = viewModel.error

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var memberToDelete by remember { mutableStateOf<Member?>(null) }
    var memberToUpdate by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchAllMembers()
    }

    fun resetForm() {
        name = ""
        phone = ""
        code = ""
        address = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Register New Member", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { input -> phone = input.filter { it.isDigit() } },
                label = { Text("Phone") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { input -> code = input.filter { it.isDigit() } },
                label = { Text("Code") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.weight(1f)
            )
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error, color = Color.Red)
        }

        Spacer(Modifier.height(16.dp))

        // Register Button with smaller width (wrap content + centered)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || code.isBlank() || address.isBlank()) {
                        viewModel.error = "All fields are required."
                        return@Button
                    }
                    val member = Member(
                        name = name,
                        phone = phone,
                        code = code.toIntOrNull() ?: 0,
                        address = address
                    )
                    viewModel.registerMember(member,
                        onSuccess = { resetForm() },
                        onError = { viewModel.error = it }
                    )
                },
                modifier = Modifier.widthIn(min = 160.dp, max = 240.dp) // smaller width, fixed range
            ) {
                Text("Register Member")
            }
        }

        Spacer(Modifier.height(32.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Registered Members", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(members) { member ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Member info column (Name, Phone, Code, Address)
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Name: ${member.name}", style = MaterialTheme.typography.bodyLarge)
                            Text("Phone: ${member.phone}", style = MaterialTheme.typography.bodyMedium)
                            Text("Code: ${member.code}", style = MaterialTheme.typography.bodyMedium)
                            Text("Address: ${member.address}", style = MaterialTheme.typography.bodyMedium)
                        }

                        // Right side: Buttons stacked vertically
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(IntrinsicSize.Min)
                        ) {
                            Button(
                                onClick = {
                                    memberToUpdate = member
                                    name = member.name
                                    phone = member.phone
                                    code = member.code.toString()
                                    address = member.address
                                },
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text("Update")
                            }

                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                onClick = { memberToDelete = member },
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${memberToDelete?.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        memberToDelete?.let { viewModel.deleteMember(it.id) }
                        memberToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update member dialog remains unchanged
    if (memberToUpdate != null) {
        AlertDialog(
            onDismissRequest = { memberToUpdate = null },
            title = { Text("Update Member") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input -> phone = input.filter { c -> c.isDigit() } },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input -> code = input.filter { c -> c.isDigit() } },
                        label = { Text("Code") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isBlank() || phone.isBlank() || code.isBlank() || address.isBlank()) {
                            viewModel.error = "All fields are required."
                            return@TextButton
                        }
                        val updatedMember = memberToUpdate!!.copy(
                            name = name,
                            phone = phone,
                            code = code.toIntOrNull() ?: 0,
                            address = address
                        )
                        viewModel.updateMember(updatedMember)
                        memberToUpdate = null
                        resetForm()
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    memberToUpdate = null
                    resetForm()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
