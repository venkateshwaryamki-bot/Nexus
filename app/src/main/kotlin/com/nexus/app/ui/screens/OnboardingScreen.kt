package com.nexus.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType

/**
 * Screen 02 — Player registration. Local-first: guest mode is the real, fully functional path.
 * Google / email sign-in need the future backend; they are shown disabled with an honest note.
 */
@Composable
fun OnboardingScreen(onComplete: (String, Int) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text("NEXUS", style = NexusType.Hero, color = Color.White)
            Text("EXPLORER REGISTRATION", style = NexusType.Label, color = NexusColors.ElectricCyan)
            Spacer(Modifier.height(32.dp))
            if (step == 0) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    SectionLabel("Choose how to begin")
                    Spacer(Modifier.height(8.dp))
                    Text("Start immediately as a guest. Your universe is stored on this device and works fully offline.",
                        style = NexusType.Body, color = NexusColors.TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    NexusPrimaryButton("Continue as guest", { step = 1 }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    NexusSecondaryButton("Continue with Google", {}, Modifier.fillMaxWidth(), enabled = false)
                    Spacer(Modifier.height(10.dp))
                    NexusSecondaryButton("Continue with email", {}, Modifier.fillMaxWidth(), enabled = false)
                    Spacer(Modifier.height(10.dp))
                    Text("Cloud accounts and sync arrive with the NEXUS online services. They are not available in this version.",
                        style = NexusType.Small, color = NexusColors.TextMuted)
                }
            } else {
                GlassPanel(Modifier.fillMaxWidth()) {
                    SectionLabel("Explorer callsign")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { if (it.length <= 20) name = it },
                        singleLine = true, placeholder = { Text("e.g. Nova", style = NexusType.Body) },
                        textStyle = NexusType.BodyStrong.copy(color = Color.White),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexusColors.ElectricCyan, unfocusedBorderColor = NexusColors.PanelBorder, cursorColor = NexusColors.ElectricCyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Insignia")
                    Spacer(Modifier.height(12.dp))
                    for (row in 0 until 2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0 until 3) {
                                val id = row * 3 + col
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { avatar = id }.padding(8.dp)) {
                                    AvatarEmblem(id, size = 62.dp, selected = avatar == id,
                                        modifier = if (avatar == id) Modifier.border(2.dp, AvatarAccents[id], CircleShape) else Modifier)
                                    Spacer(Modifier.height(4.dp))
                                    Text(AvatarNames[id].uppercase(), style = NexusType.Label, color = if (avatar == id) AvatarAccents[id] else NexusColors.TextMuted)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    NexusPrimaryButton("Confirm explorer", { onComplete(name, avatar) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
