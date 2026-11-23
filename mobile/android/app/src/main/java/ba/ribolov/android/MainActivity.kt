package ba.ribolov.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FieldHome() } }
    }
}

@Composable
private fun FieldHome() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(24.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.field_service), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.preparation), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.not_ready), style = MaterialTheme.typography.bodyMedium)
        }
    }
}