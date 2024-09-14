# SnapDetect - an object recognition app through image.

Developed an object recognition application for Android using TensorFlow Lite and MobileNet V3 model to 
identify objects through images used Retrofit for fetch data.

A real-time object recognition feature, enhancing user interaction and engagement.

## Demo Video

[![SnapDetect Demo Video](https://img.youtube.com/vi/v7t7c2WBi1M/hqdefault.jpg)](https://youtube.com/shorts/v7t7c2WBi1M?si=sbQ9ZKE5gLNt20Wt)


## API Reference
References:

Model: MobileNet V3 ( https://shorturl.at/VEnYT )

API: Knowledge Graph Search ( https://shorturl.at/Jlbb1 )
#### Dependencies 

```
    implementation("com.github.bumptech.glide:glide:4.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.tensorflow:tensorflow-lite:2.7.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.2.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.7.0")
```

#### Get item

```
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface KnowledgeGraphApiService {

    @GET("v1/entities:search")
    fun getEntityDetails(
        @Query("query") query: String,
        @Query("key") apiKey: String
    ): Call<KnowledgeGraphResponse>
}
```
#### Convert to GSON

```
    val retrofit = Retrofit.Builder()
            .baseUrl("https://kgsearch.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

```


## Features

📸 Capture images via camera

🖼️ Select images from the gallery

🌐 Use images from URLs

📤 Share images and details seamlessly


## Tech Stack

**Development Environment:** Android Studio

**Programming Language:** Kotlin

**Framework:** TensorFlow Lite

**API Integration:** Knowledge Graph Search

**Model:** MobileNet V3

**User Interface Design:** Material Design Principles

**Testing:** Unit Testing, Integration Testing, User Testing
