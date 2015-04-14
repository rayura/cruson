package com.cruson.review;

import java.io.IOException;

public interface CrucibleApi {
    public boolean isUserExist(String user) throws IOException;

    public String getUserByCommiter(String repository, String commiter)
            throws IOException;

    public void addReviewer(String reviewId, String user) throws IOException;

    public String createReview(String project, String message,
            String description, String author) throws IOException;

    public void startReview(String reviewId) throws IOException;

    public String addReviewItem(String repository, String reviewId,
            String path, String revision) throws IOException;

    public void addReviewComment(String reviewId, String itemId,
            String message, String line) throws IOException;
}