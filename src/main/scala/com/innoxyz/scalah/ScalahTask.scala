package com.innoxyz.scalah

import com.sun.tools.javah.NativeHeaderTool
import java.util.Locale

object ScalahTask extends NativeHeaderTool.NativeHeaderTask{
  
    def setLocale(locale:Locale):Unit={}
    def call():java.lang.Boolean ={false}
}