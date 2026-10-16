package com.example.taller_2.components
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.taller_2.R

/**
 * Campo de texto para escribir una dirección.
 * Cuando el usuario termina de editar (tecla "Buscar" del teclado o la lupa)
 * se llama a onSearch y se cierra el teclado.
 */
@Composable
fun AddressSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    fun doSearch() {
        focusManager.clearFocus() // oculta el teclado
        onSearch()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(stringResource(id = R.string.label_address)) },
        placeholder = { Text(stringResource(id = R.string.placeholder_address)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = stringResource(id = R.string.cd_place_icon),
            )
        },
        trailingIcon = {
            IconButton(onClick = { doSearch() }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.cd_search),
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
    )
}
