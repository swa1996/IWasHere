package ru.isu.swa.diplom.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.model.Message;

//Отображение сообщений с пользователем/группой - адаптер для RecyclerView
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Message> messages;
    private SharedPreferences mKeys;
    private SharedPreferences mSet;
    private int row;
    private Context context;
    private Integer USER_ID;


    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mainLayout;
        TextView author;
        TextView message;
        LinearLayout layoutMark;
        TextView textMark;


        public ChatViewHolder(View v) {
            super(v);
            mainLayout = (LinearLayout) v.findViewById(R.id.chatLinearLayout);
            author = (TextView) v.findViewById(R.id.authorChat);
            message = (TextView) v.findViewById(R.id.messageChat);
            layoutMark = (LinearLayout) v.findViewById(R.id.LayoutMarkInChat);
            textMark = (TextView) v.findViewById(R.id.textView4);
        }
    }

    public ChatAdapter(List<Message> mes, int rowLayout, Context context, SharedPreferences mKeys, SharedPreferences mSet, Integer userId) {
        this.mKeys = mKeys;
        this.messages = mes;
        this.row = rowLayout;
        this.context = context;
        this.USER_ID = userId;
        this.mSet = mSet;
    }

    @Override
    public ChatAdapter.ChatViewHolder onCreateViewHolder(ViewGroup parent,
                                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(row, parent, false);
        return new ChatAdapter.ChatViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final ChatAdapter.ChatViewHolder holder, final int position) {
      final Message message = messages.get(position);
        if(message.getAuthor().getId().equals(USER_ID)){
            holder.author.setTextColor(context.getResources().getColor(R.color.colorRed));
            holder.mainLayout.setBackgroundColor(context.getResources().getColor(R.color.md_light_blue_200));
            holder.mainLayout.setGravity(Gravity.RIGHT);
            holder.author.setGravity(Gravity.RIGHT);
            holder.message.setGravity(Gravity.RIGHT);
            holder.textMark.setGravity(Gravity.RIGHT);
        }
else {
            holder.author.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
            holder.mainLayout.setBackgroundColor(context.getResources().getColor(R.color.md_light_blue_50));
            holder.mainLayout.setGravity(Gravity.LEFT);
            holder.author.setGravity(Gravity.LEFT);
            holder.message.setGravity(Gravity.LEFT);
            holder.textMark.setGravity(Gravity.LEFT);
        }
        holder.author.setText(message.getAuthor().getFirstName()+" "+message.getAuthor().getLastName());
        holder.message.setText(Function.getDate(message.getDate())+":\n"+message.getMessage());
       if(message.getMark()==null){
            holder.layoutMark.setVisibility(View.INVISIBLE);
        }else{
         holder.textMark.setText("Нажмите, чтобы открыть");
         holder.textMark.setTextColor(context.getResources().getColor(R.color.md_blue_grey_300));
         holder.layoutMark.setTag(message.getMark());
         new Thread(new Runnable() {
             @Override
             public void run() {
                 if(message.getMark().isEncrypted() && !mKeys.contains("KEY_"+message.getMark().getId())){
                     String key = message.getKey().get(mSet.getInt("USER_PUBLICKEY_ID",0));
                     if(key!=null){
                         String secretKey = mSet.getString("USER_PRIVATEKEY","");
                         try {
                             KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                             PKCS8EncodedKeySpec rsaKey = new PKCS8EncodedKeySpec(Function.hexStringToByteArray(secretKey));
                             PrivateKey rsaKey2 = keyFactory.generatePrivate(rsaKey);
                             Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                             c.init(Cipher.DECRYPT_MODE, rsaKey2);
                             String s = new String(c.doFinal(Function.hexStringToByteArray(key)));
                             final SharedPreferences.Editor editor = mKeys.edit();
                             editor.putString("KEY_"+message.getMark().getId(), s);
                             editor.commit();
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
             }
         }).start();


         }
        }


    @Override
    public int getItemCount() {
        if (messages == null) return 0;
        return messages.size();
    }

}
