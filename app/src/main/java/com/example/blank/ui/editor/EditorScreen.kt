package com.example.blank.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.blank.editor.MarkdownHighlighter

@Composable
fun EditorScreen(
    noteId: String,
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlighter = remember { MarkdownHighlighter() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val editTextRef = remember { mutableStateOf<AppCompatEditText?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        key(noteId) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    AppCompatEditText(context).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setTextColor(android.graphics.Color.BLACK)
                        textSize = 18f
                        gravity = Gravity.TOP or Gravity.START
                        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                        setPadding(0, 0, 0, 0)
                        setLineSpacing(0f, 1.35f)
                        setText(content)
                        setSelection(text?.length ?: 0)
                        highlighter.apply(text ?: return@apply)
                        requestFocus()
                        post { keyboard?.show() }
                    }.also { editText ->
                        val watcher = object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) {
                                if (s == null) return
                                highlighter.apply(s)
                                onContentChange(s.toString())
                            }
                        }
                        editText.addTextChangedListener(watcher)
                        editText.setTag(watcher)
                    }
                },
                update = { editText ->
                    editTextRef.value = editText
                }
            )
        }
    }

    LaunchedEffect(noteId) {
        editTextRef.value?.requestFocus()
        keyboard?.show()
    }

    DisposableEffect(Unit) {
        onDispose {
            editTextRef.value?.clearFocus()
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
}
