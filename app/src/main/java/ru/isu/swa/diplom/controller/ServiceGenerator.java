package ru.isu.swa.diplom.controller;



import java.net.CookieManager;
import java.net.CookiePolicy;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swa on 01.02.2018.
 */
//Класс для отправки запросов через Retrofit API
public class ServiceGenerator {
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    public static final String BASE_URL = "https://iwshere.xyz/";
    private static Retrofit.Builder builder(String url) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create());
    }

    private static Retrofit retrofit=null;

    public static <S> S createService(Class<S> serviceClass, String url) {
        return retrofit(url).create(serviceClass);
    }


    public static Retrofit retrofit(String url){
        if (retrofit == null) {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
            OkHttpClient client = httpClient.build();
            retrofit = builder(url).client(client).build();
        }
        return retrofit;
    }
}

