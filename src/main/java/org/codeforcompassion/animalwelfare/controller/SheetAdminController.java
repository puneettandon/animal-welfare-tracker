package org.codeforcompassion.animalwelfare.controller;


import lombok.RequiredArgsConstructor;
import org.codeforcompassion.animalwelfare.service.GoogleSheetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sheets")
public class SheetAdminController {

    private final GoogleSheetService googleSheetService;

    @PostMapping("/reorder-articles")
    public ResponseEntity<String> reorderArticleSheets() {
        try {
            googleSheetService.reorderMonthlySheets();
            return ResponseEntity.ok("Sheets reordered successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
