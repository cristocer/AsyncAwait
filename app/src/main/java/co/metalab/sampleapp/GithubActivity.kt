package co.metalab.sampleapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import co.metalab.metaroutine.AsyncController
import co.metalab.metaroutine.asyncUI
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_github.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class GitHubActivity : AppCompatActivity() {
    private val TAG = GitHubActivity::class.java.simpleName

    var retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    var github = retrofit.create(GitHubService::class.java)

    var reposList = emptyList<Repo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github)
        btnGetRepos.setOnClickListener { refreshRepos() }
    }

    private fun refreshRepos() = asyncUI {
        txtRepos.text = ""
        pbGetRepos.visibility = View.VISIBLE
        btnGetRepos.isEnabled = false
        txtStatus.text = "Loading repos list..."

        val userName = txtUserName.text.toString()
        reposList = await(github.listRepos(userName))
        showRepos(reposList)

        reposList.forEach {
            txtStatus.text = "Loading info for ${it.name}..."
            val repoDetails = await(github.repoDetails(userName, it.name))
            it.stars = repoDetails.stargazers_count
            showRepos(reposList)
        }

        pbGetRepos.visibility = View.INVISIBLE
        btnGetRepos.isEnabled = true
        txtStatus.text = "Done."
    }.onError {
        pbGetRepos.visibility = View.INVISIBLE
        btnGetRepos.isEnabled = true

        val errorMessage = if (it is RetrofitHttpException) {
            Gson().fromJson(it.errorBody.string(), GithubErrorResponse::class.java).message
        } else {
            "Couldn't load repos (${it.message})"
        }

        txtStatus.text = errorMessage
        Log.e(TAG, errorMessage, it)
    }

    private fun showRepos(reposResponse: List<Repo>) {
        val reposList = reposResponse.joinToString(separator = "\n") {
            val starsCount = if (it.stars == null) "" else {
                " * ${it.stars}"
            }
            it.name + starsCount
        }
        txtRepos.text = reposList
    }
}

interface GitHubService {
    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Call<List<Repo>>

    @GET("repos/{user}/{repo}")
    fun repoDetails(@Path("user") user: String, @Path("repo") repo: String): Call<Repo>
}

data class Repo(val name: String, val stargazers_count: Int, var stars: Int? = null)

data class GithubErrorResponse(val message: String, val documentation_url: String)