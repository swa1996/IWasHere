package ru.isu.swa.diplom.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.GPSLocation;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Coordinate;
import ru.isu.swa.diplom.model.Mark;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Создание новой метки
public class NewMarkActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView textNewMark;
    private TextView fieldNewMark;
    private CheckBox isAnonymed;
    private CheckBox isEncrypted;
    private Button button;
    private Coordinate coord;
    private GPSLocation location;
    private AccountHeader headerResult;
    private Drawer result;
    private SharedPreferences mSettings;
    private SharedPreferences mKeys;
    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_mark);
        textNewMark = (TextView) findViewById(R.id.textProfile);
        fieldNewMark = (TextView) findViewById(R.id.fieldTextMark);
        isAnonymed = (CheckBox) findViewById(R.id.isAnonymed);
        isEncrypted = (CheckBox) findViewById(R.id.isEncrypted);
        button = (Button) findViewById(R.id.buttonNewMark);
        button.setOnClickListener(this);
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        mKeys = getSharedPreferences("KEYS", Context.MODE_PRIVATE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Для работы приложения необходимо разрешить доступ к местоположению", Toast.LENGTH_LONG).show();
            return;
        }
        location = GPSLocation.GPSLocationHolder.getGPSLocation(this);
        coord = GPSLocation.getCoords();
        if(coord==null)button.setEnabled(false);
        else {
            textNewMark.setText("Ваше местоположение: широта - " + coord.getLatitude() + ", долгота - " + coord.getLongitude() + ", точность - " + GPSLocation.getAccuracy()+" м.");
        }
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Добавить метку");
        final IProfile profile = new ProfileDrawerItem().withName(mSettings.getString("USER_FIRSTNAME","")+" "+mSettings.getString("USER_LASTNAME","")).withEmail(mSettings.getString("USER_EMAIL",""));
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
                            switch((int)drawerItem.getIdentifier()){
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

                    }}).withSavedInstance(savedInstanceState).build();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);

    }

    @Override
    public void onClick(final View v) {
        if(fieldNewMark.getText().toString().isEmpty()) Toast.makeText(this, "Введите текст", Toast.LENGTH_LONG).show();
        else{
            if(coord==null)Toast.makeText(this, "Ошибка определения местоположения", Toast.LENGTH_LONG).show();
            else{
                //создаём объект Mark
                Mark mark = new Mark();
                final SharedPreferences.Editor editor = mKeys.edit();
                if(isEncrypted.isChecked()){
                    mark.setEncrypted(true);
                    try {
                        KeyGenerator kg = KeyGenerator.getInstance("AES");
                        SecureRandom sr = new SecureRandom();
                        kg.init(sr);
                        SecretKey key = kg.generateKey();

                        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        byte[] IV = new byte[16];
                        sr.nextBytes(IV);
                        c.init(Cipher.ENCRYPT_MODE, key, new
                                IvParameterSpec(IV));
                        editor.putString("KEY_TEMP", Function.bytesToHexString(key.getEncoded())+"/"+Function.bytesToHexString(IV));
                        editor.commit();

                        byte[] encryptedData = c.doFinal(fieldNewMark.getText().toString().getBytes());
                        mark.setMessage(Function.bytesToHexString(encryptedData));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    }
                }else{
                    mark.setMessage(fieldNewMark.getText().toString());
                    mark.setEncrypted(false);
                }Integer id = mSettings.getInt("USER_ID", 0);
                mark.setCoords(coord);
                mark.setAnonymed(isAnonymed.isChecked());
                //отправка объекта на сервер
                Call<Mark> call = apiService.addMark(mSettings.getString("USER_COOKIE",""),mSettings.getInt("USER_ID",0), mark);
                call.enqueue(new Callback<Mark>() {
                    @Override
                    public void onResponse(Call<Mark> call, Response<Mark> response) {
                        int statusCode = response.code();
                        switch (statusCode) {
                            case 200:
                                final Mark mark = response.body();
                              if(mark.isEncrypted()) {
                                  runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          editor.putString("KEY_" + mark.getId(), mKeys.getString("KEY_TEMP",""));
                                          editor.remove("KEY_TEMP");
                                          editor.commit();
                                      }
                                  });
                              }
                                //перенаправление на MarkActivity
                                Intent intent = new Intent(getApplicationContext(), MarkActivity.class);
                              intent.putExtra("mark_id", mark.getId());
                              startActivity(intent);
                                break;
                            case 401:
                                Toast.makeText(getApplicationContext(),"Сессия устарела",Toast.LENGTH_LONG).show();
                                Function.cleanCookie(mSettings);
                                startActivity(new Intent(getApplicationContext(),AuthActivity.class));
                                break;

                            case 404:
                                Toast.makeText(v.getContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                break;
                            case 500:
                                Toast.makeText(v.getContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                break;
                            case 400:
                                Toast.makeText(v.getContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(v.getContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Mark> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), t.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}
