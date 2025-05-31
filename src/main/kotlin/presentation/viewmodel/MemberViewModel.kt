package presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.model.Member
import data.repository.DuplicateCodeException
import data.repository.MemberRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MemberViewModel() : KoinComponent {

    private val repository: MemberRepository by inject()
    val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

     var members by mutableStateOf<List<Member>>(emptyList())


    var error by mutableStateOf<String?>(null)

    fun fetchAllMembers() {
        coroutineScope.launch {
            try {
                members = repository.getAllMembers()
            } catch (e: Exception) {
                error = "Error fetching members: ${e.message}"
            }
        }
    }

    fun registerMember(member: Member, onSuccess: () -> Unit, onError: (String) -> Unit) {
        coroutineScope.launch {
            try {
                // Optional pre-check (can be removed if you prefer to rely on DB constraint)
                if (repository.isCodeExists(member.code)) {
                    throw DuplicateCodeException("Code already exists")
                }

                repository.addMember(member)
                fetchAllMembers()
                onSuccess()
            } catch (e: DuplicateCodeException) {
                onError("Member code must be unique! ${e.message}")
            } catch (e: Exception) {
                onError("Error registering member: ${e.message}")
            }
        }
    }

    fun deleteMember(memberId: Int) {
        coroutineScope.launch {
            repository.deleteMember(memberId)
            fetchAllMembers()
        }
    }

    fun updateMember(member: Member) {
        coroutineScope.launch {
            repository.updateMember(member)
        }
        fetchAllMembers()
    }
}
