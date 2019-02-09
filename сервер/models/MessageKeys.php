<?php

namespace app\models;
use yii\db\ActiveRecord;

class MessageKeys extends ActiveRecord
{
public static function tableName(){
    return "message_keys";
}
public function getKey()
{
    return $this->hasOne(Key::className(), ['id' => 'key_id']);
}
public function setKey($key){
    $this->key = $key;
}
}
?>