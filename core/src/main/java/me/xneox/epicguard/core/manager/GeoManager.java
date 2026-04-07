/*
 * EpicGuard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EpicGuard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.xneox.epicguard.core.manager;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import me.xneox.epicguard.core.EpicGuard;
import me.xneox.epicguard.core.util.LogUtils;
import me.xneox.epicguard.core.util.TextUtils;
import org.jetbrains.annotations.NotNull;

public final class GeoManager {
  private final EpicGuard epicGuard;

  private DatabaseReader countryReader;
  private DatabaseReader cityReader;

  public GeoManager(EpicGuard epicGuard) {
    this.epicGuard = epicGuard;
    epicGuard.logger().info("This product includes GeoLite2 data created by MaxMind, available from https://www.maxmind.com");

    final var parent = epicGuard.getFolderPath().resolve("data");
    if (Files.notExists(parent)) {
      try {
        Files.createDirectories(parent);
      } catch (IOException ignored) {}
    }

    final var countryDatabase = parent.resolve("GeoLite2-Country.mmdb");
    final var cityDatabase = parent.resolve("GeoLite2-City.mmdb");

    try {
      if (Files.exists(countryDatabase)) {
        this.countryReader = new DatabaseReader.Builder(countryDatabase.toFile()).withCache(new CHMCache()).build();
        epicGuard.logger().info("GeoIP Country database loaded successfully.");
      }

      if (Files.exists(cityDatabase)) {
        this.cityReader = new DatabaseReader.Builder(cityDatabase.toFile()).withCache(new CHMCache()).build();
        epicGuard.logger().info("GeoIP City database loaded successfully.");
      }
    } catch (IOException ex) {
      LogUtils.catchException("Error while loading the GeoIP databases from files.", ex);
    }
  }

  @NotNull
  public String countryCode(final @NotNull String address) {
    final var inetAddress = TextUtils.parseAddress(address);
    if (inetAddress != null && this.countryReader != null) {
      try {
        final String isoCode = this.countryReader.country(inetAddress).getCountry().getIsoCode();
        if (isoCode != null) {
          return isoCode;
        }
      } catch (IOException | GeoIp2Exception ex) {
        this.epicGuard.logger().warn("Couldn't find the country for the address {}: {}", address, ex.getMessage());
      }
    }
    return "unknown";
  }

  @NotNull
  public String city(final @NotNull String address) {
    final var inetAddress = TextUtils.parseAddress(address);
    if (inetAddress != null && this.cityReader != null) {
      try {
        final String city = this.cityReader.city(inetAddress).getCity().getName();
        if (city != null) {
          return city;
        }
      } catch (IOException | GeoIp2Exception ex) {
        this.epicGuard.logger().warn("Couldn't find the city for the address {}: {}", address, ex.getMessage());
      }
    }
    return "unknown";
  }
}
