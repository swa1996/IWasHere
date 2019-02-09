package ru.isu.swa.diplom.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.icons.MaterialDrawerFont;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import org.w3c.dom.Text;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.isu.swa.diplom.MainActivity;
import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.controller.Function;
import ru.isu.swa.diplom.controller.IWHApi;
import ru.isu.swa.diplom.controller.ServiceGenerator;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.Key;
import ru.isu.swa.diplom.model.Mark;
import ru.isu.swa.diplom.model.Message;
import ru.isu.swa.diplom.model.User;

import static ru.isu.swa.diplom.controller.ServiceGenerator.BASE_URL;

//Просмотр метки
public class MarkActivity extends AppCompatActivity implements View.OnClickListener {
LinearLayout authorLink;
TextView author;
TextView date;
TextView message;
Mark mark;
Menu menu;
final Integer TIME_ON_CHANGE_MARK = 3600000;
final Integer TIME_ON_DELETE_MARK = 86400000;
private Integer USER_ID;
private String USER_COOKIE;

    private static IWHApi apiService = ServiceGenerator.createService(IWHApi.class, BASE_URL);;
    private SharedPreferences mKeys;
    private SharedPreferences mSettings;
    private Drawer result;
    private AccountHeader headerResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark);
        authorLink = (LinearLayout) findViewById(R.id.linearLayout);
        author = (TextView) findViewById(R.id.author);
        date = (TextView) findViewById(R.id.date);
        message = (TextView) findViewById(R.id.message);
        message.setMovementMethod(ScrollingMovementMethod.getInstance());
        authorLink.setOnClickListener(this);
        mSettings = getSharedPreferences("USER", Context.MODE_PRIVATE);
        USER_ID = mSettings.getInt("USER_ID",0);
        USER_COOKIE = mSettings.getString("USER_COOKIE","");
        mKeys = getSharedPreferences("KEYS", Context.MODE_PRIVATE);

        //получение информации о метке
        Call<Mark> call = apiService.getMark(USER_COOKIE,USER_ID, getIntent().getIntExtra("mark_id",0));
        call.enqueue(new Callback<Mark>() {
            @Override
            public void onResponse(Call<Mark> call, Response<Mark> response) {
                int statusCode = response.code();
                switch (statusCode) {
                    case 200:
                        mark = response.body();
                        Date dateOfMark = new Date();
                        dateOfMark.setTime(Long.parseLong(mark.getDate())*1000);
                        Date currentDate = new Date();
                        MenuItem change = menu.findItem(2);
                        MenuItem delete = menu.findItem(3);
                        if(mark==null || currentDate.getTime() - dateOfMark.getTime() > TIME_ON_CHANGE_MARK || (!mark.isAnonymed() && !mark.getAuthor().getId().equals(USER_ID))) change.setVisible(false);
                        if(mark==null || currentDate.getTime() - dateOfMark.getTime() > TIME_ON_DELETE_MARK || (!mark.isAnonymed() && !mark.getAuthor().getId().equals(USER_ID))) delete.setVisible(false);
                        if(mark.getAuthor()==null || mark.isAnonymed()) {
                        author.setText("Аноним");
                        }else{
                            author.setText(mark.getAuthor().getFirstName()+" "+mark.getAuthor().getLastName());
                        }
                        date.setText(Function.getDate(mark.getDate()));
                        if(mark.isEncrypted()) {
                            String[] key = mKeys.getString("KEY_"+mark.getId(), "").split("/");
                            if(key==null || key.length!=2){
                                message.setText("Ошибка! Отсутствует ключ шифрования");
                                message.setTextColor(getResources().getColor(R.color.colorRed));
                            }
                            else{
                                byte[] keyByte = Function.hexStringToByteArray(key[0]);
                                SecretKey key1 = new SecretKeySpec(keyByte, "AES");
                                byte[] IV = Function.hexStringToByteArray(key[1]);
                                byte[] cbyte = Function.hexStringToByteArray(mark.getMessage());

                                Cipher c = null;
                                try {
                                    c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                    c.init(Cipher.DECRYPT_MODE, key1, new
                                            IvParameterSpec(IV));

                                    byte[] decryptedData = c.doFinal(cbyte);
                                    message.setText(new String(decryptedData));
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


                            }}
                        else {
                            message.setText(mark.getMessage());
                        }
                        break;
                    case 401:
                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                        intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                        Function.cleanCookie(mSettings);
                        startActivity(intent1);
                        break;

                    case 404:
                        Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                        break;
                    case 400:
                        Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                        break;
                    case 500:
                        Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Mark> call, Throwable t) {
                String mes = t.getMessage();
            }
        });
        //боковое меню
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setSubtitle("Просмотр метки");
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

        //боковое меню закончилось
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, UserActivity.class);
        Integer id = 0;
        if(!mark.isAnonymed() && mark.getAuthor()!=null)id=mark.getAuthor().getId();
        intent.putExtra("user_id", id);
        startActivity(intent);
    }
    //боковое меню
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
            case 1:
                shareMark();
                break;
            case 2:
                changeMark();
                return true;
            case 3:
                deleteMark();
                return true;
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
        menu.add(1, 1, 1, "Поделиться");
        this.menu = menu;
        menu.add(1, 2, 1, "Изменить");
        menu.add(1, 3, 1, "Удалить");
        return super.onCreateOptionsMenu(menu);
    }
    public void deleteMark(){

        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle("Удалить метку");  // заголовок
        ad.setMessage("Вы действительно хотите удалить метку?\n Отменить это действие будет невозможно"); // сообщение
        ad.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                //удаление метки
                Call<Void> call = apiService.deleteMark(USER_COOKIE, USER_ID, mark.getId());
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        int statusCode = response.code();
                        switch (statusCode) {
                            case 200:
                                Toast.makeText(getApplicationContext(), "Метка успешно удалена", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                break;
                            case 401:
                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                Function.cleanCookie(mSettings);
                                startActivity(intent1);
                                break;
                            case 403:
                                Toast.makeText(getApplicationContext(), "Метку может удалить только её автор", Toast.LENGTH_LONG).show();
                                break;
                            case 408:
                                Toast.makeText(getApplicationContext(), "Метку можно удалить только в течение 24 часов после добавления", Toast.LENGTH_LONG).show();
                                break;
                            case 404:
                                Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                break;
                            case 400:
                                Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                                break;
                            case 500:
                                Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        String mes = t.getMessage();
                    }
                });
            }
        });
        ad.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
            }
        });
        //Создаем AlertDialog:
        AlertDialog alertDialog = ad.create();

        //и отображаем его:
        alertDialog.show();
    }
    public void changeMark(){
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.layout_edit_mark, null);

        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);

        mDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.fieldTextMark);
        userInput.setText(message.getText());
        final CheckBox isEncrypted = (CheckBox) promptsView.findViewById(R.id.isEncrypted);
        isEncrypted.setChecked(mark.isEncrypted());
        final CheckBox isAnonymed = (CheckBox) promptsView.findViewById(R.id.isAnonymed);
        isAnonymed.setChecked(mark.isAnonymed());
        mDialogBuilder
                .setCancelable(true)
                .setPositiveButton("Изменить",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                mark.setAnonymed(isAnonymed.isChecked());
                                mark.setEncrypted(isEncrypted.isChecked());
                                if(mark.isEncrypted()){
                                        String[] key = mKeys.getString("KEY_" + mark.getId(), "").split("/");
                                        try{
                                            SecretKey key1;
                                            byte[] IV = new byte[16];
                                        if (key == null || key.length != 2) {
                                            KeyGenerator kg = KeyGenerator.getInstance("AES");
                                            SecureRandom sr = new SecureRandom();
                                            kg.init(sr);
                                            key1 = kg.generateKey();
                                            sr.nextBytes(IV);
                                            SharedPreferences.Editor editor = mKeys.edit();
                                            editor.putString("KEY_"+mark.getId(), Function.bytesToHexString(key1.getEncoded())+"/"+Function.bytesToHexString(IV));
                                            editor.commit();
                                        } else {
                                            byte[] keyByte = Function.hexStringToByteArray(key[0]);
                                             key1 = new SecretKeySpec(keyByte, "AES");
                                             IV = Function.hexStringToByteArray(key[1]);
                                        }
                                            byte[] cbyte = Function.hexStringToByteArray(mark.getMessage());


                                            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                            c.init(Cipher.ENCRYPT_MODE, key1, new
                                                    IvParameterSpec(IV));

                                            byte[] encryptedData = c.doFinal(userInput.getText().toString().getBytes());
                                            mark.setMessage(Function.bytesToHexString(encryptedData));

                                    } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        } catch (InvalidKeyException e) {
                                            e.printStackTrace();
                                        } catch (InvalidAlgorithmParameterException e) {
                                            e.printStackTrace();
                                        } catch (NoSuchPaddingException e) {
                                            e.printStackTrace();
                                        } catch (BadPaddingException e) {
                                            e.printStackTrace();
                                        } catch (IllegalBlockSizeException e) {
                                            e.printStackTrace();
                                        }
                                }else{
                                    mark.setMessage(userInput.getText().toString());
                                }
                                Call<Mark> call = apiService.changeMark(USER_COOKIE,USER_ID, mark);
                                call.enqueue(new Callback<Mark>() {
                                    @Override
                                    public void onResponse(Call<Mark> call, Response<Mark> response) {
                                        int statusCode = response.code();
                                        switch (statusCode) {
                                            case 200:
                                                mark = response.body();
                                                Toast.makeText(getApplicationContext(), "Метка изменена", Toast.LENGTH_LONG).show();
                                                message.setText(userInput.getText());
                                                if(mark.isAnonymed())author.setText("Аноним");
                                                else author.setText(mark.getAuthor().getFirstName()+" "+mark.getAuthor().getLastName());
                                                break;
                                            case 401:
                                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                Function.cleanCookie(mSettings);
                                                intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                                startActivity(intent1);
                                                break;
                                            case 403:
                                                Toast.makeText(getApplicationContext(), "Метку может изменить только её автор", Toast.LENGTH_LONG).show();
                                                break;
                                            case 408:
                                                Toast.makeText(getApplicationContext(), "Метку можно изменить только в течение 1 часа после добавления", Toast.LENGTH_LONG).show();
                                                break;
                                                case 404:
                                                Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                                break;
                                            case 400:
                                                Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
                                                break;
                                            case 500:
                                                Toast.makeText(getApplicationContext(), "Ошибка сервера", Toast.LENGTH_LONG).show();
                                                break;
                                            default:
                                                Toast.makeText(getApplicationContext(), "Неизвестная ошибка", Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Mark> call, Throwable t) {
                                        String mes = t.getMessage();
                                    }
                                });

                            }
                        })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        //Создаем AlertDialog:
        AlertDialog alertDialog = mDialogBuilder.create();

        //и отображаем его:
        alertDialog.show();

    }
    public void shareMark(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Поделиться")
                .setCancelable(true)
                .setPositiveButton("Отправить другу", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Call<List<User>> call = apiService.getFriends(USER_COOKIE, USER_ID);
                        call.enqueue(new Callback<List<User>>() {
                            @Override
                            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                                int code = response.code();
                                switch (code) {
                                    case 200:
                                       final List<User> friends = response.body();
                                       final boolean checked[] = new boolean[friends.size()];
                                       for(int i=0;i<checked.length;i++){
                                           checked[i] = false;
                                       }
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MarkActivity.this);
                                        builder.setTitle("Выбор получателей")
                                              .setCancelable(true)
                                                .setMultiChoiceItems(Function.getNames(friends), checked, new DialogInterface.OnMultiChoiceClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                        checked[which] = isChecked;
                                                    }
                                                })
                                                .setPositiveButton("Продолжить", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        final Message mes = new Message();
                                                        mes.setMark(mark);
                                                        final Set<User> receiver = new HashSet();
                                                        final Map<Integer, String> keys = new HashMap();
                                                        for(int i=0;i<checked.length;i++){
                                                            if(checked[i]) receiver.add(friends.get(i));
                                                        }
                                                        if(mark.isEncrypted()) {
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    for (final User user : receiver) {
                                                                        new Thread(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                for (Key key : user.getKeys()) {
                                                                                    try {
                                                                                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                                                                        X509EncodedKeySpec rsaKey = new X509EncodedKeySpec(Function.hexStringToByteArray(key.getKey()));
                                                                                        PublicKey rsaKey2 = keyFactory.generatePublic(rsaKey);
                                                                                        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                                                                        c.init(Cipher.ENCRYPT_MODE, rsaKey2);
                                                                                        String s = Function.bytesToHexString(c.doFinal(mKeys.getString("KEY_" + mark.getId(), "").getBytes()));
                                                                                        keys.put(key.getId(), s);
                                                                                    } catch (NoSuchAlgorithmException e) {
                                                                                        e.printStackTrace();
                                                                                    } catch (InvalidKeySpecException e) {
                                                                                        e.printStackTrace();
                                                                                    } catch (NoSuchPaddingException e) {
                                                                                        e.printStackTrace();
                                                                                    } catch (IllegalBlockSizeException e) {
                                                                                        e.printStackTrace();
                                                                                    } catch (BadPaddingException e) {
                                                                                        e.printStackTrace();
                                                                                    } catch (InvalidKeyException e) {
                                                                                        e.printStackTrace();
                                                                                    }

                                                                                }
                                                                            }
                                                                        }).start();
                                                                    }
                                                                    mes.setKey(keys);
                                                                }
                                                            }).start();


                                                        }
                                                        mes.setReceivers(receiver);
                                                        LayoutInflater li = LayoutInflater.from(getApplicationContext());
                                                        View promptsView = li.inflate(R.layout.layout_sendmessage, null);

                                                        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MarkActivity.this);

                                                        mDialogBuilder.setView(promptsView);

                                                        final EditText userInput = (EditText) promptsView.findViewById(R.id.newMessage);

                                                        mDialogBuilder
                                                                .setCancelable(true)
                                                                .setPositiveButton("Отправить",
                                                                        new DialogInterface.OnClickListener() {
                                                                            public void onClick(DialogInterface dialog,int id) {
                                                                                mes.setMessage(userInput.getText().toString());
                                                                                Call<Message> call = apiService.sendMessage(USER_COOKIE, USER_ID, mes);
                                                                                call.enqueue(new Callback<Message>() {
                                                                                    @Override
                                                                                    public void onResponse(Call<Message> call, Response<Message> response) {
                                                                                        int statusCode = response.code();
                                                                                        switch (statusCode) {
                                                                                            case 200:
                                                                                                Toast.makeText(getApplicationContext(), "Сообщение успешно отравлено", Toast.LENGTH_LONG).show();
                                                                                                break;
                                                                                            case 401:
                                                                                                Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                                                                Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                                                                Function.cleanCookie(mSettings);
                                                                                                intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                                                                                startActivity(intent1);
                                                                                                break;
                                                                                            case 403:
                                                                                                Toast.makeText(getApplicationContext(), "Вы не можете отправить сообщение этому пользователю", Toast.LENGTH_LONG).show();
                                                                                                break;
                                                                                            case 404:
                                                                                                Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                                                                                break;
                                                                                            case 400:
                                                                                                Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
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
                                                                                        String mes = t.getMessage();
                                                                                    }
                                                                                });

                                                                            }
                                                                        })
                                                                .setNegativeButton("Отмена",
                                                                        new DialogInterface.OnClickListener() {
                                                                            public void onClick(DialogInterface dialog,int id) {
                                                                                dialog.cancel();
                                                                            }
                                                                        });

                                                        //Создаем AlertDialog:
                                                        AlertDialog alertDialog = mDialogBuilder.create();

                                                        //и отображаем его:
                                                        alertDialog.show();
                                                    }
                                                })
                                                .setNegativeButton("Отмена",
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        });
                                        AlertDialog alert = builder.create();
                                        alert.show();
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
                            public void onFailure(Call<List<User>> call, Throwable t) {
                                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Отправить группе",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Call<List<Group>> call = apiService.getGroups(USER_COOKIE, USER_ID);
                                call.enqueue(new Callback<List<Group>>() {
                                    @Override
                                    public void onResponse(Call<List<Group>> call, Response<List<Group>> response) {
                                        int code = response.code();
                                        switch (code) {
                                            case 200:
                                                final List<Group> groups = response.body();
                                                AlertDialog.Builder builder = new AlertDialog.Builder(MarkActivity.this);
                                                builder.setTitle("Выбор группы")
                                                        .setCancelable(true)
                                                        .setItems(Function.getNames(groups), new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                final Message mes = new Message();
                                                                mes.setMark(mark);
                                                                final Group group = groups.get(which);
                                                                mes.setGroupReceiver(group);
                                                                final Map<Integer, String> keys = new HashMap();
                                                                if(mark.isEncrypted()) {
                                                                    new Thread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            for (final User user : group.getMembers()) {
                                                                                new Thread(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        for (Key key : user.getKeys()) {
                                                                                            try {
                                                                                                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                                                                                X509EncodedKeySpec rsaKey = new X509EncodedKeySpec(Function.hexStringToByteArray(key.getKey()));
                                                                                                PublicKey rsaKey2 = keyFactory.generatePublic(rsaKey);
                                                                                                Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                                                                                c.init(Cipher.ENCRYPT_MODE, rsaKey2);
                                                                                                String s = Function.bytesToHexString(c.doFinal(mKeys.getString("KEY_" + mark.getId(), "").getBytes()));
                                                                                                keys.put(key.getId(), s);
                                                                                            } catch (NoSuchAlgorithmException e) {
                                                                                                e.printStackTrace();
                                                                                            } catch (InvalidKeySpecException e) {
                                                                                                e.printStackTrace();
                                                                                            } catch (NoSuchPaddingException e) {
                                                                                                e.printStackTrace();
                                                                                            } catch (IllegalBlockSizeException e) {
                                                                                                e.printStackTrace();
                                                                                            } catch (BadPaddingException e) {
                                                                                                e.printStackTrace();
                                                                                            } catch (InvalidKeyException e) {
                                                                                                e.printStackTrace();
                                                                                            }

                                                                                        }
                                                                                    }
                                                                                }).start();
                                                                            }
                                                                            mes.setKey(keys);
                                                                        }
                                                                    }).start();

                                                                }
                                                                LayoutInflater li = LayoutInflater.from(getApplicationContext());
                                                                View promptsView = li.inflate(R.layout.layout_sendmessage, null);

                                                                AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MarkActivity.this);

                                                                mDialogBuilder.setView(promptsView);

                                                                final EditText userInput = (EditText) promptsView.findViewById(R.id.newMessage);

                                                                mDialogBuilder
                                                                        .setCancelable(true)
                                                                        .setPositiveButton("Отправить",
                                                                                new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog,int id) {
                                                                                        mes.setMessage(userInput.getText().toString());
                                                                                        Call<Message> call = apiService.sendMessage(USER_COOKIE, USER_ID, mes);
                                                                                        call.enqueue(new Callback<Message>() {
                                                                                            @Override
                                                                                            public void onResponse(Call<Message> call, Response<Message> response) {
                                                                                                int statusCode = response.code();
                                                                                                switch (statusCode) {
                                                                                                    case 200:
                                                                                                        Toast.makeText(getApplicationContext(), "Сообщение успешно отравлено", Toast.LENGTH_LONG).show();
                                                                                                        break;
                                                                                                    case 401:
                                                                                                        Toast.makeText(getApplicationContext(), "Сессия устарела", Toast.LENGTH_LONG).show();
                                                                                                        Intent intent1 = new Intent(getApplicationContext(), AuthActivity.class);
                                                                                                        Function.cleanCookie(mSettings);
                                                                                                        intent1.putExtra("email", mSettings.getString("USER_EMAIL",""));
                                                                                                        startActivity(intent1);
                                                                                                        break;
                                                                                                    case 403:
                                                                                                        Toast.makeText(getApplicationContext(), "Вы не можете отправить сообщение этой группе", Toast.LENGTH_LONG).show();
                                                                                                        break;
                                                                                                    case 404:
                                                                                                        Toast.makeText(getApplicationContext(), "Пользователь не найден", Toast.LENGTH_LONG).show();
                                                                                                        break;
                                                                                                    case 400:
                                                                                                        Toast.makeText(getApplicationContext(), "Неверный запрос", Toast.LENGTH_LONG).show();
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
                                                                                                String mes = t.getMessage();
                                                                                            }
                                                                                        });
                                                            }
                                                        });

                                                                //Создаем AlertDialog:
                                                                AlertDialog alertDialog = mDialogBuilder.create();

                                                                //и отображаем его:
                                                                alertDialog.show();
                                                            }
                                                        })
                                                        .setNegativeButton("Отмена",
                                                                new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                        dialog.cancel();
                                                                    }
                                                                });
                                                AlertDialog alert = builder.create();
                                                alert.show();
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
                                    public void onFailure(Call<List<Group>> call, Throwable t) {
                                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                             }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }


}
