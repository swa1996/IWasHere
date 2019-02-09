<?php

namespace app\models;
use yii\db\ActiveRecord;

class Mark extends ActiveRecord
{
public static function tableName(){
    return "mark";
}
public function getAuthor()
{
    return $this->hasOne(User::className(), ['id' => 'author']);
}
public function getCoords()
{
    return $this->hasOne(Coordinate::className(), ['id' => 'coords']);
}
public function setAuthor(User $author){
    $this->author = $author;
}
public function setCoords(Coordinate $coords){
    $this->coords = $coords;
}
}
?>