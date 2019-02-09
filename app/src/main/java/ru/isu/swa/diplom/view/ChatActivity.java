package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.ChatAdapter;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Coordinate;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.Mark;
import ru.isu.swa.diplom.model.Message;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Чат с выбранным юзером/группой
public class ChatActivity extends AppCompatActivity {
    private SharedPreferences mSettings;
    private SharedPreferences mKeys;
    private Drawer result;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AccountHeader headerResult;
    private Integer USER_ID;
    private String USER_COOKIE;
    private User user;
    private User otherUser;
    private Integer SUB_ID;
    private String sub;
    private Group group;
    private Menu menu;
    private IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);
    private MapView mapView;
    private TextView newMessage;
    private TextView button;
    private LinearLayout layoutMap;
    private Button buttonCloseMap;
    private String API_KEY = "118e902b-33ac-4c09-894b-7a4fbfb49b73";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.setApiKey(API_KEY);
        setContentView(R.layout.activity_chat);
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        mKeys = getSharedPreferences("KEYS", Context.MODE_PRIVATE);
        USER_ID = mSettings.getInt("USER_ID", 0);
        USER_COOKIE = mSettings.getString("USER_COOKIE", "");
        SUB_ID = getIntent().getIntExtra("sub_id", 0);
        sub = getIntent().getStringExtra("sub");

        MapKitFactory.initialize(this);
        mapView = (MapView) findViewById(R.id.mapview);
        newMessage = (TextView) findViewById(R.id.editTextChat);
        layoutMap = (LinearLayout) findViewById(R.id.layoutMapView);
        buttonCloseMap = (Button) findViewById(R.id.buttonCloseMap);
        buttonCloseMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMap.setVisibility(View.GONE);
                mSwipeRefreshLayout.setVisibility(View.VISIBLE);
            }
        });
        button = (Button) findViewById(R.id.buttonNewMessage);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message mes = new Message();
                mes.setAuthor(user);
                if(group!=null){
                    mes.setGroupReceiver(group);
                if(!group.getMembers().contains(user)){
                    //юзер не состоит в группе
                    Toast.makeText(getApplicationContext(), "Вы не можете отправлять сообшения этой группе", Toast.LENGTH_LONG).show();
                    return;
                }
                }
                if(otherUser!=null) {
                    Set<User> receivers = new HashSet();
                    receivers.add(otherUser);
                    mes.setReceivers(receivers);
                    if(!otherUser.getFriends().contains(user)){
                        //Юзеры не являются друзьями
                        Toast.makeText(getApplicationContext(), "Вы не можете отправлять сообшения этому пользователю", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                mes.setMessage(newMessage.getText().toString());
                Call<Message> call = apiService.sendMessage(USER_COOKIE, USER_ID, mes);
                //Обрабатываем ответ от сервера на запрос
                call.enqueue(new Callback<Message>() {
                    @Override
                    public void onResponse(Call<Message> call, Response<Message> response) {
                        int statusCode = response.code();
                        switch (statusCode) {
                            case 200:
                                Toast.makeText(getApplicationContext(), "Ваше сообщение отправлено", Toast.LENGTH_LONG).show();
                                newMessage.setText("");
                                refresh();
                                break;
                            case 401:
                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                Function.cleanCookie(mSettings);
                                startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                                break;
                            case 404:
                                Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                break;
                            case 500:
                                Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();

                        }
                    }

                    @Override
                    public void onFailure(Call<Message> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();

                    }
                });
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                user = Function.getUser(USER_COOKIE, USER_ID, USER_ID);
                if (sub.equals("user")) otherUser = Function.getUser(USER_COOKIE, USER_ID, SUB_ID);
                if(sub.equals("group")) group = Function.getGroup(USER_COOKIE, USER_ID, SUB_ID);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if((otherUser!=null && !otherUser.getFriends().contains(user)) || (group!=null && !group.getMembers().contains(user))){
                            button.setEnabled(false);
                            newMessage.setEnabled(false);
                        }
                    }
                });
            }
        }).start();
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final IProfile profile = new ProfileDrawerItem().withName(mSettings.getString("USER_FIRSTNAME", "") + " " + mSettings.getString("USER_LASTNAME", "")).withEmail(mSettings.getString("USER_EMAIL", ""));
        headerResult = new AccountHeaderBuilder().withHeaderBackground(R.color.colorPrimaryDark)
                .withActivity(this)
                .withCompactStyle(true)
                .addProfiles(
                        profile
                )
                .withSavedInstance(savedInstanceState)
                .build();
        result = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Метки").withIdentifier(1),
                        new PrimaryDrawerItem().withName("Профиль").withIdentifier(2),
                        new PrimaryDrawerItem().withName("Друзья").withIdentifier(3),
                        new PrimaryDrawerItem().withName("Группы").withIdentifier(4),
                        new PrimaryDrawerItem().withName("Сообщения").withIdentifier(5),
                        new SecondaryDrawerItem().withName("Выход").withIdentifier(6)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
//обработка выбранного меню
                            switch ((int) drawerItem.getIdentifier()) {
                                case 1:
                                    //метки
                                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                    break;
                                case 2:
                                    //Переход на профиль
                                    startActivity(new Intent(getApplicationContext(),ProfileActivity.class));
                                    break;
                                case 3:
                                    //Друзья
                                    startActivity(new Intent(getApplicationContext(),FriendActivity.class));
                                    break;
                                case 4:
                                    //Группы
                                    startActivity(new Intent(getApplicationContext(), GroupListActivity.class));
                                    break;
                                case 5:
                                    //Сообщения
                                    startActivity(new Intent(getApplicationContext(), MessageActivity.class));
                                    break;
                                case 6:
                                    //Выход
                                    Function.exit(view.getContext(), mSettings);
                                    break;
                            }
                        }
                        return false;

                    }
                }).withSavedInstance(savedInstanceState).build();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
        recyclerView = (RecyclerView) findViewById(R.id.chatRView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });
        //Получаем компоненту SwipeRefreshLayout для управления поведением при обновлении списка
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Обновляем данные по сети
                refresh();
            }
        });
        refresh();

    }

    protected void onSaveInstanceState(Bundle outState) {
        outState = result.saveInstanceState(outState);
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 1:
                //профиль пользователя
                if(sub.equals("user")) {
                    Intent intent = new Intent(this, UserActivity.class);
                    intent.putExtra("user_id", SUB_ID);
                    startActivity(intent);
                }
                //группа
                if(sub.equals("group")) {
                    Intent intent = new Intent(this, GroupActivity.class);
                    intent.putExtra("group_id", SUB_ID);
                    startActivity(intent);
                }
                break;
            default:
        }
                return super.onOptionsItemSelected(item);
        }

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.add(1, 1, 1, "Открыть профиль");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        getSupportActionBar().setSubtitle("Чат");
        Call<List<Message>> call = apiService.getMessagesWith(USER_COOKIE, USER_ID, SUB_ID, sub);
        mSwipeRefreshLayout.setRefreshing(true);
        //Обрабатываем ответ от сервера на запрос
        call.enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                int statusCode = response.code();
                switch (statusCode) {
                    case 200:
                        List<Message> messages = response.body();
                       recyclerView.setAdapter(new ChatAdapter(messages, R.layout.list_chat, getApplicationContext(), mKeys, mSettings, USER_ID));
                        break;
                    case 401:
                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                        Function.cleanCookie(mSettings);
                        startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                        break;
                    case 404:
                        Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                        break;
                    case 500:
                        Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();

            }
        });
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        MapKitFactory.getInstance().onStart();
    }

    public void onClickMessage(View v) {
        Mark mark = (Mark) v.getTag();
        Coordinate coords = mark.getCoords();
        mSwipeRefreshLayout.setVisibility(View.GONE);
        layoutMap.setVisibility(View.VISIBLE);
        Point point = new Point(coords.getLatitude(), coords.getLongitude());

        mapView.getMap().move(
                new CameraPosition(point, 18.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 2),
                null);
        MapObjectCollection mapObjects = mapView.getMap().getMapObjects().addCollection();
        PlacemarkMapObject markObject = mapObjects.addPlacemark(point);
        markObject.setOpacity(0.5f);
        markObject.setIcon(ImageProvider.fromResource(this, R.drawable.mark));
        markObject.setDraggable(true);
    }
}