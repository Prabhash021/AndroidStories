package com.example.androidstories;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.mylibrary.Stories;
import com.example.mylibrary.StoryItem;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Stories storiesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storiesView = findViewById(R.id.stories);

        List<StoryItem> storiesList = Arrays.asList(
                new StoryItem("https://assets.mixkit.co/videos/preview/mixkit-paddleboarder-1166-large.mp4?story_type=video", 1),
                new StoryItem("https://www.hiphopshakespeare.com/wp-content/uploads/2013/11/dummy-image-portrait.jpg?story_type=img", 0),
                new StoryItem("https://assets.mixkit.co/videos/preview/mixkit-green-leaves-and-branches-of-a-tree-out-of-focus-34376-large.mp4?story_type=video", 1),
                new StoryItem("https://as1.ftcdn.net/v2/jpg/01/48/86/26/1000_F_148862610_DMP962w9X6y0Zbl5EYPhV4Hn906LraMv.jpg?story_type=img", 0),
                new StoryItem("https://assets.mixkit.co/videos/preview/mixkit-waves-in-the-water-1164-large.mp4?story_type=video", 1),
                new StoryItem("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4?story_type=video", 1)
        );

        storiesView.setStoriesList(storiesList);
    }
}