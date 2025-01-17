package org.example.todoapp.data

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.example.todoapp.domain.RequestState
import org.example.todoapp.domain.TodoTask

class MongoDB {
    private var realm: Realm? = null

    init {
        configureRealm()
    }

    private fun configureRealm() {
       if(realm == null || realm!!.isClosed() ) {
           val config = RealmConfiguration.Builder(
               schema = setOf(TodoTask::class)
           )
               .compactOnLaunch()
               .build()
           realm = Realm.open(config)
       }
    }

    fun readActiveTasks() : Flow<RequestState<List<TodoTask>>> {
        return realm?.query<TodoTask>(query = "Completed == $0" , false)
            ?.asFlow()
            ?.map { result ->
                RequestState.Success(
                    data = result.list.sortedByDescending { task -> task.favorite }
                )

            } ?: flow {
                RequestState.Error(message = "Realm is not available")
        }
    }

    fun readCompletedTasks() : Flow<RequestState<List<TodoTask>>> {
        return realm?.query<TodoTask>(query = "Completed == $0" , true)
            ?.asFlow()
            ?.map { result ->
                RequestState.Success(
                    data = result.list
                )

            } ?: flow {
            RequestState.Error(message = "Realm is not available")
        }
    }

    suspend fun addTask(task: TodoTask) {
        realm?.write {
            copyToRealm(task)
        }
    }

    suspend fun updateTask(task: TodoTask) {
        realm?.write {
            try {
                val queriedTask = query<TodoTask>("_id == $0", task._id)
                    .first()
                    .find()
                queriedTask?.let {
                    findLatest(it)?.let { currentTask ->
                        currentTask.title = task.title
                        currentTask.description = task.description
                    }
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    suspend fun setCompleted(task: TodoTask, taskCompleted: Boolean) {
        realm?.write {
            try {
                val queriedTask = query<TodoTask>(query = "_id == $0", task._id)
                    .find()
                    .first()
                queriedTask.apply { Completed = taskCompleted }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    suspend fun setFavorite(task: TodoTask, isFavorite: Boolean) {
        realm?.write {
            try {
                val queriedTask = query<TodoTask>(query = "_id == $0", task._id)
                    .find()
                    .first()
                queriedTask.apply { favorite = isFavorite }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    suspend fun deleteTask(task: TodoTask) {
        realm?.write {
            try {
                val queriedTask = query<TodoTask>(query = "_id == $0", task._id)
                    .first()
                    .find()
                queriedTask?.let {
                    findLatest(it)?.let { currentTask ->
                        delete(currentTask)
                    }
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }


}