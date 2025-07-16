package com.example.dealspy.controller;

import com.example.dealspy.auth.AuthUtils;
import com.example.dealspy.common.ApiResponse;
import com.example.dealspy.dto.SaveForLaterDTO;
import com.example.dealspy.dto.UserDetailDTO;
import com.example.dealspy.dto.WatchlistDTO;
import com.example.dealspy.dto.WatchlistResponseDTO;
import com.example.dealspy.service.SaveForLaterService;
import com.example.dealspy.service.UserService;
import com.example.dealspy.service.WatchListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private WatchListService watchListService;
    @Autowired
    private SaveForLaterService saveForLaterService;


    @GetMapping("/profile")
    public UserDetailDTO getUserProfile() {
        String uid = AuthUtils.getCurrentUserId();
        return userService.getUser(uid);
    }

    @GetMapping("/watchlist")
    public ResponseEntity<ApiResponse<List<WatchlistResponseDTO>>> getWatchList() {
        String uid = AuthUtils.getCurrentUserId();
        List<WatchlistResponseDTO> list = watchListService.getUserWatchList(uid);
        ApiResponse<List<WatchlistResponseDTO>> response = new ApiResponse<>(true, "watchlist fetched successfully", list);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/watchlist")
    public ResponseEntity<ApiResponse<Void>> addToWatchList(@RequestBody WatchlistDTO watchlist) {
        String uid = AuthUtils.getCurrentUserId();
        watchListService.addToWatchList(uid, watchlist);
        ApiResponse<Void> response = new ApiResponse<>(true, "Product added on watchlist successfully", null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/watchlist/{productName}")
    public ResponseEntity<ApiResponse<Void>> deleteFromWatchlist(@PathVariable String productName){
        String uid = AuthUtils.getCurrentUserId();
        watchListService.deleteFromWatchList(uid,productName);
        ApiResponse<Void> response = new ApiResponse<>(true,"Product removed from watchlist",null);
        return ResponseEntity.ok(response);
    }


    // GET SaveForLater
    @GetMapping("/saveforlater")
    public ResponseEntity<ApiResponse<List<SaveForLaterDTO>>> getSaveForLater() {
        String uid = AuthUtils.getCurrentUserId();
        List<SaveForLaterDTO> list = saveForLaterService.getUserSaveForLater(uid);

        ApiResponse<List<SaveForLaterDTO>> response = new ApiResponse<>(
                true,
                "Save For Later fetched successfully",
                list
        );
        return ResponseEntity.ok(response);
    }

    // POST SaveForLater
    @PostMapping("/saveforlater")
    public ResponseEntity<ApiResponse<Void>> addToSaveForLater(@RequestBody SaveForLaterDTO dto) {
        String uid = AuthUtils.getCurrentUserId();

        String message = saveForLaterService.addToSaveForLater(uid, dto);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                message,
                null
        );
        return ResponseEntity.ok(response);
    }

    // DELETE SaveForLater
    @DeleteMapping("/saveforlater/{productName}")
    public ResponseEntity<ApiResponse<Void>> deleteFromSaveForLater(@PathVariable String productName) {
        String uid = AuthUtils.getCurrentUserId();

        saveForLaterService.deleteFromSaveForLater(uid, productName);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
               "Product removed from Save for later Successfully",
                null
        );
        return ResponseEntity.ok(response);
    }

}
