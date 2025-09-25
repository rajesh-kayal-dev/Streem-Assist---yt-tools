// PageController.java - Updated to include transcript route
package com.YouTubeTools.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/video-details")
    public String videoDetails() {
        return "video-details";
    }

    @GetMapping("/tags")
    public String SEOTagsAnalysis() {
        return "tags";
    }

//    @GetMapping("/transcript")
//    public String transcript() {
//        return "transcript";
//    }
}