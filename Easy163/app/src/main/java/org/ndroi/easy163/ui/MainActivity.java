package org.ndroi.easy163.ui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.ndroi.easy163.BuildConfig;
import org.ndroi.easy163.R;
import org.ndroi.easy163.core.Cache;
import org.ndroi.easy163.core.Local;
import org.ndroi.easy163.utils.EasyLog;
import org.ndroi.easy163.vpn.LocalVPNService;
import static androidx.appcompat.app.AlertDialog.Builder;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ToggleButton.OnCheckedChangeListener
{
    private ToggleButton toggleButton = null;
    private static boolean isBroadcastReceived = false; // workaround for multi-receive
    public static void resetBroadcastReceivedState()
    {
        isBroadcastReceived = false;
    }
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (isBroadcastReceived) return;
            isBroadcastReceived = true;
            boolean isServiceRunning = intent.getBooleanExtra("isRunning", false);
            Log.d("MainActivity", "BroadcastReceiver service isRunning: " + isServiceRunning);
            toggleButton.setChecked(isServiceRunning);
            if(isServiceRunning)
            {
                EasyLog.log("Easy163 VPN 正在运行");
                EasyLog.log("原作者: ndroi, 此版本由溯洄w4123修改");
            }else
            {
                EasyLog.log("Easy163 VPN 停止运行");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, new IntentFilter("service"));
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        toggleButton = findViewById(R.id.bt_start);
        toggleButton.setOnCheckedChangeListener(this);
        EasyLog.setTextView(findViewById(R.id.log));
        syncServiceState();
        intentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK)
                    {
                        Intent intent = new Intent(MainActivity.this, LocalVPNService.class);
                        ContextCompat.startForegroundService(this, intent);
                    }
                }
        );
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            //super.onBackPressed();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.nav_github)
        {
            Uri uri = Uri.parse("https://github.com/ndroi/easy163");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (id == R.id.nav_usage)
        {
            Builder builder = new Builder(this);
            builder.setTitle("使用说明");
            builder.setMessage("开启本软件 VPN 服务后即可使用\n" +
                    "如无法启动 VPN 尝试重启手机\n" +
                    "出现异常问题尝试情况软件缓存\n" +
                    "更多问题请查阅 Github");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_statement)
        {
            Builder builder = new Builder(this);
            builder.setTitle("免责声明");
            builder.setMessage("本软件为实验性项目\n" +
                    "仅提供技术研究使用\n" +
                    "本软件完全免费\n" +
                    "作者不承担用户因软件造成的一切责任");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (id == R.id.nav_clear_cache)
        {
            Cache.clear();
            Local.clear();
            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about)
        {
            Builder builder = new Builder(this);
            builder.setTitle("关于");
            builder.setMessage("当前版本 " + BuildConfig.VERSION_NAME + "\n" +
                    "版本更新关注 Github Release");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            startVPN();
        } else
        {
            stopVPN();
        }
    }

    private void syncServiceState()
    {
        Intent intent = new Intent("control");
        intent.putExtra("cmd", "check");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startVPN()
    {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            intentActivityResultLauncher.launch(vpnIntent);
        else {
            Intent intent = new Intent(this, LocalVPNService.class);
            ContextCompat.startForegroundService(this, intent);
        }
    }

    private void stopVPN()
    {
        Intent intent = new Intent("control");
        intent.putExtra("cmd", "stop");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d("stopVPN", "try to stopVPN");
    }

}