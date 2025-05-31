package data.repository

import data.model.Member
import database.Members
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DuplicateCodeException(message: String) : Exception(message)

interface MemberRepository {
    suspend fun getAllMembers(): List<Member>
    suspend fun addMember(member: Member)  // Changed to throw exception
    suspend fun isCodeExists(code: Int): Boolean  // New method
    suspend fun deleteMember(memberId: Int)
    suspend fun updateMember(member: Member): Int
}


class MemberRepositoryImpl : MemberRepository {

    override suspend fun getAllMembers(): List<Member> = transaction {
        Members.selectAll().map {
            Member(
                id = it[Members.id].value,
                phone = it[Members.phone],
                name = it[Members.name],
                code = it[Members.code],
                address = it[Members.address]
            )
        }
    }

    override suspend fun addMember(member: Member) {
        transaction {
            // Check for existing code first
            if (Members.select { Members.code eq member.code }.count() > 0) {
                throw DuplicateCodeException("Member code ${member.code} already exists")
            }

            Members.insert {
                it[phone] = member.phone
                it[name] = member.name
                it[code] = member.code
                it[address] = member.address
            }
        }
    }

    override suspend fun isCodeExists(code: Int): Boolean = transaction {
        Members.select { Members.code eq code }.count() > 0
    }

    override suspend fun deleteMember(memberId: Int) {
        transaction {
            Members.deleteWhere { Members.id eq memberId }
        }
    }

    override suspend fun updateMember(member: Member) = transaction {
        Members.update({ Members.id eq member.id }) {
            it[code] = member.code
            it[name] = member.name
            it[phone] = member.phone
            it[address] = member.address
        }
    }
}