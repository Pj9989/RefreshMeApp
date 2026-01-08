package com.refreshme.stylist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.ui.theme.RefreshMeTheme

class SubscriptionActivity : ComponentActivity() {
    private val functions = FirebaseFunctions.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                SubscriptionScreen(
                        onSubscribeClick = { priceId ->
                            createSubscription(priceId)
                        },
                        onBack = { finish() }
                )
            }
        }
    }

    private fun createSubscription(priceId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val data = hashMapOf(
                "userId" to user.uid,
                "priceId" to priceId
        )

        // Show loading toast
        Toast.makeText(this, "Processing subscription...", Toast.LENGTH_SHORT).show()

        functions
            .getHttpsCallable("createSubscription")
            .call(data)
            .addOnSuccessListener { result ->
                Toast.makeText(this, "Subscription successful!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun SubscriptionScreen(onSubscribeClick: (String) -> Unit, onBack: () -> Unit) {
    var selectedPriceId by remember { mutableStateOf("price_1Slum6HjAO6voCTJQ9Lc0ggB") } // Default to Basic

    val plans = listOf(
            Plan("Basic", "$10/month", "price_1Slum6HjAO6voCTJQ9Lc0ggB", listOf("Standard Profile", "Basic Analytics", "Email Support")),
            Plan("Pro", "$25/month", "price_pro_id_here", listOf("Featured Listing", "Advanced Analytics", "24/7 Priority Support", "Custom Branding"))
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
        ) {
            Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                    text = "Select the best plan for your business growth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans.size) { index ->
                    val plan = plans[index]
                    PlanCard(
                            plan = plan,
                            isSelected = selectedPriceId == plan.priceId,
                            onSelect = { selectedPriceId = plan.priceId }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = { onSubscribeClick(selectedPriceId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text("Subscribe Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Restore Purchase", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PlanCard(plan: Plan, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() },
            shape = RoundedCornerShape(16.dp),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = plan.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = plan.price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = feature, fontSize = 14.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

data class Plan(val name: String, val price: String, val priceId: String, val features: List<String>)
