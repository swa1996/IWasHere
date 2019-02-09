package ru.isu.swa.diplom.controller;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.Key;
import ru.isu.swa.diplom.model.Mark;
import ru.isu.swa.diplom.model.Message;
import ru.isu.swa.diplom.model.User;

/**
 * Created by swa on 01.02.2018.
 */

//Интерфейс для сетевого взаимодействия через Retrofit API. Содержит все методы запросов к серверу.
public interface IWHApi {
    //получение пароля на email
    @FormUrlEncoded
    @POST("index.php?r=api/auth")
    Call<Void> getPassword(@Field("email") String email);

    //аутентификация
    @FormUrlEncoded
    @POST("index.php?r=api/auth2")
    Call<User> auth(@Field("email") String email, @Field("password") String password);

    //регистрация
    @POST("index.php?r=api/reg")
    Call<User> register(@Body User user);

    //изменение профиля юзера
    @FormUrlEncoded
    @POST("index.php?r=api/change-user")
    Call<User> changeUser(@Header("Cookie") String cookie, @Query("user_id") Integer id, @Field("phoneNumber") String phoneNumber, @Field("firstName") String firstName, @Field("lastName") String lastName);

    //добавление нового открытого ключа для юзера
    @POST("index.php?r=api/add-key")
    Call<Key> addKey(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Key key);

    //добавление в список друзей
    @FormUrlEncoded
    @POST("index.php?r=api/add-friend")
    Call<User> addFriend(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("friend_id") Integer friendId);

    //удаление из списка друзей
    @FormUrlEncoded
    @POST("index.php?r=api/delete-friend")
    Call<User> deleteFriend(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("friend_id") Integer friendId);

    //информация о юзере
    @FormUrlEncoded
    @POST("index.php?r=api/user-info")
    Call<User> getUserInfo(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("sub_id") Integer friendId);

    //информация о метке
    @FormUrlEncoded
    @POST("index.php?r=api/get-mark")
    Call<Mark> getMark(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("mark_id") Integer markId);

    //информация о группе
    @FormUrlEncoded
    @POST("index.php?r=api/get-group")
    Call<Group> getGroup(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("group_id") Integer groupId);

    //добавление новой метки
    @POST("index.php?r=api/add-mark")
    Call<Mark> addMark(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Mark mark);

    //изменение метки
    @POST("index.php?r=api/change-mark")
    Call<Mark> changeMark(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Mark mark);

    //удаление метки
    @FormUrlEncoded
    @POST("index.php?r=api/delete-mark")
    Call<Void> deleteMark(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("mark_id") Integer mark_id);

    //получение всех меток для заданной точки
    @FormUrlEncoded
    @POST("index.php?r=api/get-marks")
    Call<List<Mark>> getMarks(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("latitude") Double latitude, @Field("longitude") Double longitude, @Field("accuracy") Double accuracy);

    //отправка нового сообщения
    @POST("index.php?r=api/new-message")
    Call<Message> sendMessage(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Message mes);

    //Создание новой группы
    @POST("index.php?r=api/add-group")
    Call<Group> addGroup(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Group group);

    //изменение параметров группы
    @POST("index.php?r=api/change-group")
    Call<Group> chengeGroup(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Body Group group);

    //добавление админа в группу
    @FormUrlEncoded
    @POST("index.php?r=api/add-administrator")
    Call<Group> addAdministrator(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("group_id") Integer groupId, @Field("admin_id") Integer adminId);

    //удаление пользователя группы из админов
    @FormUrlEncoded
    @POST("index.php?r=api/delete-administrator")
    Call<Group> deleteAdministrator(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("group_id") Integer groupId, @Field("admin_id") Integer adminId);

    //добавление нового юзера в группу
    @FormUrlEncoded
    @POST("index.php?r=api/add-member")
    Call<Group> addMember(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("group_id") Integer groupId, @Field("member_id") Integer memberId);

    //удаление юзера из группы
    @FormUrlEncoded
    @POST("index.php?r=api/delete-member")
    Call<Group> deleteMember(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("group_id") Integer groupId, @Field("member_id") Integer memberId);

    //получение групп для текущего пользователя
    @POST("index.php?r=api/get-groups")
    Call<List<Group>> getGroups(@Header("Cookie") String cookie,@Query("user_id") Integer id);

    //получение сообщений(отправленных и принятых) для текущего пользователя и всех его групп
    @POST("index.php?r=api/get-messages")
    Call<List<Message>> getMessages(@Header("Cookie") String cookie,@Query("user_id") Integer id);

    //список "взаимных друзей"
    @POST("index.php?r=api/get-friends")
    Call<List<User>> getFriends(@Header("Cookie") String cookie,@Query("user_id") Integer id);

    //получение чата с пользователем или группой
    @POST("index.php?r=api/get-messages-with")
    Call<List<Message>> getMessagesWith(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Query("sub_id") Integer subId, @Query("sub") String sub);

    //Выход с текущего устройства
    @FormUrlEncoded
    @POST("index.php?r=api/exit")
    Call<Void> exit(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("key_id") Integer keyId);

    //Выйти на всех устройствах
    @FormUrlEncoded
    @POST("index.php?r=api/exit-all")
    Call<Void> exitAll(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("key_id") Integer keyId);

    //Поиск юзеров
    @FormUrlEncoded
    @POST("index.php?r=api/search-user")
    Call<List<User>> searchUser(@Header("Cookie") String cookie,@Query("user_id") Integer id, @Field("str") String str);

}
