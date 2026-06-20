package com.morsel.constants;

public final class ApiPaths {

    private ApiPaths() {}

    public static final String API_V1 = "/api/v1";

    public static final String AUTH = API_V1 + "/auth";
    public static final String AUTH_WILDCARD = AUTH + "/**";

    public static final String RECIPES = API_V1 + "/recipes";
    public static final String RECIPES_WILDCARD = RECIPES + "/**";

    public static final String IMAGES = API_V1 + "/images";
    public static final String IMAGES_WILDCARD = IMAGES + "/**";
    public static final String IMAGES_PREFIX = IMAGES + "/";

    public static final String USERS = API_V1 + "/users";

    public static final String RECIPE_COMMENTS = RECIPES + "/{recipeId}/comments";
    public static final String RECIPE_RATINGS = RECIPES + "/{recipeId}/ratings";
    public static final String RECIPE_FAVORITE = RECIPES + "/{recipeId}/favorite";

    public static final String CORS_WILDCARD = "/**";
}
