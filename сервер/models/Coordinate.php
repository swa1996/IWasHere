<?php

namespace app\models;
use yii\db\ActiveRecord;

class Coordinate extends ActiveRecord
{
public static function tableName(){
    return "coordinate";
}
}
?>