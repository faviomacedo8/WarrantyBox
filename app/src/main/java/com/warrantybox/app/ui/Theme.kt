package com.warrantybox.app.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Light=lightColorScheme(primary=Color(0xFF365F4B),secondary=Color(0xFF526A5C),tertiary=Color(0xFF3C6472),surface=Color(0xFFF8FAF7),background=Color(0xFFF6F8F5),error=Color(0xFFBA1A1A))
private val Dark=darkColorScheme(primary=Color(0xFFA1D1B5),secondary=Color(0xFFB8CCBD),tertiary=Color(0xFFA3CDDC),background=Color(0xFF101512),surface=Color(0xFF171C19))
@Composable fun WarrantyTheme(dark:Boolean,content:@Composable () -> Unit)=MaterialTheme(colorScheme=if(dark)Dark else Light,typography=Typography(),content=content)
