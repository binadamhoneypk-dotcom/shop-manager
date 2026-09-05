package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.StaffEntity
import com.example.data.entity.StaffRole
import com.example.ui.CalculatorTarget
import com.example.ui.ShopViewModel
import com.example.util.AppStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPayrollScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.language.collectAsState()
    val staffList by viewModel.shopStaff.collectAsState()
    val activeStaff by viewModel.activeStaff.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val salaryPayments by viewModel.salaryPayments.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Staff & Attendance, 1: Audit Log
    var selectedStaffForSalary by remember { mutableStateOf<StaffEntity?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    Scaffold(
        modifier = modifier.testTag("staff_payroll_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Staff & Attendance") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Audit / Activity Logs") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Current Staff Account Status
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Logged in: ${activeStaff?.name ?: "Guest"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Role: ${activeStaff?.role?.name ?: "None"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(activeStaff?.role?.name ?: "None", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Staff Roster & Today's Attendance ($todayStr)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(staffList) { staff ->
                        val todayAttendance = attendanceList.find { it.staffId == staff.id && it.date == todayStr }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = staff.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${staff.role.name} • Salary: Rs. ${staff.wageRate.toInt()}/mo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (todayAttendance?.status) {
                                            "PRESENT" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            "ABSENT" -> MaterialTheme.colorScheme.errorContainer
                                            "HALF_DAY" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = todayAttendance?.status ?: "NOT MARKED",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (todayAttendance?.status) {
                                                "PRESENT" -> Color(0xFF047857)
                                                "ABSENT" -> MaterialTheme.colorScheme.onErrorContainer
                                                "HALF_DAY" -> Color(0xFFB45309)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Divider()
                                Spacer(Modifier.height(10.dp))

                                // Attendance Marking Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "PRESENT" to "P",
                                        "ABSENT" to "A",
                                        "HALF_DAY" to "1/2",
                                        "LEAVE" to "L"
                                    ).forEach { (st, label) ->
                                        val isCurrent = todayAttendance?.status == st
                                        FilledTonalButton(
                                            onClick = { viewModel.markAttendance(staff.id, st) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { selectedStaffForSalary = staff },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Pay", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(40.dp))
                    }
                }
            } else {
                // Audit / Activity Logs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Audit Trail / Security Activity Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Monitors all changes made by staff across the shop.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                    }

                    items(activityLogs) { log ->
                        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (log.actionType) {
                                            "SALE" -> Icons.Default.ShoppingCart
                                            "PAYMENT_RECEIVED" -> Icons.Default.AttachMoney
                                            "EXPENSE" -> Icons.Default.MoneyOff
                                            "ATTENDANCE" -> Icons.Default.Schedule
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.details,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "By ${log.staffName} • $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    // Pay Salary Dialog
    if (selectedStaffForSalary != null) {
        val staff = selectedStaffForSalary!!
        var payAmountStr by remember { mutableStateOf((staff.wageRate / 2).toInt().toString()) }
        var payNote by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedStaffForSalary = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .testTag("salary_pay_dialog"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Pay Salary — ${staff.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = payAmountStr,
                            onValueChange = { payAmountStr = it },
                            label = { Text("Amount to Pay (Rs.) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.openCalculator(
                                    CalculatorTarget(
                                        fieldName = "Salary",
                                        initialValue = payAmountStr,
                                        onResult = { payAmountStr = it }
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = "Calc", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Note (e.g. Month Salary / Advance)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedStaffForSalary = null }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = payAmountStr.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    viewModel.paySalary(staff, amount, payNote)
                                    selectedStaffForSalary = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Pay")
                        }
                    }
                }
            }
        }
    }
}
