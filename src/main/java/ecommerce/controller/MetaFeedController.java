package ecommerce.controller;


import jakarta.servlet.http.HttpServletResponse;

public interface MetaFeedController {

    void generateMetaFeed(HttpServletResponse response) throws Exception;
}
