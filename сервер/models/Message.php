<?php

namespace app\models;
use yii\db\ActiveRecord;

class Message extends ActiveRecord
{
public static function tableName(){
    return "message";
}
public function getAuthor()
{
    return $this->hasOne(User::className(), ['id' => 'author']);
}
public function getGroupReceiver()
{
    return $this->hasOne(Group::className(), ['id' => 'groupReceiver']);
}
public function getMark()
{
    return $this->hasOne(Mark::className(), ['id' => 'mark']);
}
public function getReceivers()
{
    return $this->hasMany(User::className(), ['id' => 'receiver_id'])
        ->viaTable('message_receiver', ['message_id' => 'id'])->from(['u2' => User::tableName()]);
}
public function getKeys()
{
    return $this->hasMany(MessageKeys::className(), ['message_id' => 'id']);
}
public function setAuthor(User $author){
    $this->author = $author;
}
public function setGroupReceiver(Group $group){
    $this->groupReceiver = $group;
}
public function setMark(Mark $mark){
    $this->mark = $mark;
}
public function setReceivers($rec){
    $this->receivers = $rec;
}
public function setKeys($keys){
    $this->keys = $keys;
}
}
?>