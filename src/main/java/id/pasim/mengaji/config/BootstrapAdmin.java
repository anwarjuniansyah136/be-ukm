package id.pasim.mengaji.config;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class BootstrapAdmin implements CommandLineRunner {
    private final UserRepository users;
    private final ProgramRepository programs;
    private final MaterialRepository materials;
    private final HafalanTaskRepository tasks;
    private final ActivityRepository activities;
    private final HalaqahSessionRepository halaqahSessions;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public BootstrapAdmin(
            UserRepository users,
            ProgramRepository programs,
            MaterialRepository materials,
            HafalanTaskRepository tasks,
            ActivityRepository activities,
            HalaqahSessionRepository halaqahSessions,
            PasswordEncoder encoder,
            @Value("${APP_ADMIN_EMAIL:}") String email,
            @Value("${APP_ADMIN_PASSWORD:}") String password
    ) {
        this.users = users;
        this.programs = programs;
        this.materials = materials;
        this.tasks = tasks;
        this.activities = activities;
        this.halaqahSessions = halaqahSessions;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        String adminEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : "admin@pasim.ac.id";
        String adminPass = (password != null && !password.isBlank()) ? password : "Admin12345678!";

        UserAccount adminAccount = null;
        if (!users.existsByEmail(adminEmail)) {
            adminAccount = new UserAccount(
                    "Administrator Sistem",
                    adminEmail,
                    encoder.encode(adminPass),
                    Role.ADMIN,
                    Role.ADMIN,
                    RegistrationStatus.APPROVED
            );
            adminAccount.updateProfile("Administrator Sistem", "08110000001", "ADM-PASIM", "Biro Kemahasiswaan", "Pengelola operasional sistem informasi UKM PASIM Mengaji", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150");
            adminAccount = users.save(adminAccount);
        } else {
            adminAccount = users.findByEmail(adminEmail).orElse(null);
        }

        // Seed demo accounts if database is empty
        UserAccount pengurusAccount = null;
        if (!users.existsByEmail("pengurus@pasim.ac.id")) {
            pengurusAccount = new UserAccount(
                    "Ustadz Ridwan (Pengurus UKM)",
                    "pengurus@pasim.ac.id",
                    encoder.encode("Pengurus12345678!"),
                    Role.PENGURUS,
                    Role.PENGURUS,
                    RegistrationStatus.APPROVED
            );
            pengurusAccount.updateProfile("Ustadz Ridwan (Pengurus UKM)", "081234567890", "PGR-2024", "Pendidikan Agama Islam", "Ketua Pembina dan Penguji Hafalan UKM PASIM Mengaji", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150");
            pengurusAccount = users.save(pengurusAccount);
        } else {
            pengurusAccount = users.findByEmail("pengurus@pasim.ac.id").orElse(null);
        }

        UserAccount anggotaAccount = null;
        if (!users.existsByEmail("anggota@pasim.ac.id")) {
            anggotaAccount = new UserAccount(
                    "Ahmad Fauzi (Mahasiswa)",
                    "anggota@pasim.ac.id",
                    encoder.encode("Anggota12345678!"),
                    Role.ANGGOTA,
                    Role.ANGGOTA,
                    RegistrationStatus.APPROVED
            );
            anggotaAccount.updateMemberStatus(MemberStatus.AKTIF_TAHLIL);
            anggotaAccount.updateProfile("Ahmad Fauzi (Mahasiswa)", "081399887766", "23110045", "Teknik Informatika", "Mahasiswa aktif angkatan 2023, fokus memperdalam tajwid dan tahlil", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150");
            anggotaAccount = users.save(anggotaAccount);
        } else {
            anggotaAccount = users.findByEmail("anggota@pasim.ac.id").orElse(null);
            if (anggotaAccount != null && anggotaAccount.getMemberStatus() == null) {
                anggotaAccount.updateMemberStatus(MemberStatus.AKTIF_TAHLIL);
                users.save(anggotaAccount);
            }
        }

        if (!users.existsByEmail("calon@pasim.ac.id")) {
            UserAccount calon = new UserAccount(
                    "Siti Nurhaliza (Pendaftar Baru)",
                    "calon@pasim.ac.id",
                    encoder.encode("Calon12345678!"),
                    Role.ANGGOTA,
                    Role.ANGGOTA,
                    RegistrationStatus.PENDING
            );
            calon.updateProfile("Siti Nurhaliza", "081987654321", "24110012", "Sistem Informasi", "Pendaftar baru calon anggota UKM PASIM Mengaji", null);
            users.save(calon);
        }

        if (!users.existsByEmail("calon.pengurus@pasim.ac.id")) {
            UserAccount calonPengurus = new UserAccount(
                    "Maryam Ulfa (Calon Pengurus)",
                    "calon.pengurus@pasim.ac.id",
                    encoder.encode("Calon12345678!"),
                    Role.ANGGOTA,
                    Role.PENGURUS,
                    RegistrationStatus.PENDING
            );
            calonPengurus.updateProfile("Maryam Ulfa", "081555667788", "22110088", "Manajemen", "Calon pengurus divisi kurikulum dan halaqah", null);
            users.save(calonPengurus);
        }

        // Demo Murid Tartil / Tahlil (Aktif di Tahlil)
        if (!users.existsByEmail("tartil@pasim.ac.id")) {
            UserAccount tartilUser = new UserAccount(
                    "Muhammad Zaki (Murid Tartil)",
                    "tartil@pasim.ac.id",
                    encoder.encode("Anggota12345678!"),
                    Role.ANGGOTA,
                    Role.ANGGOTA,
                    RegistrationStatus.APPROVED
            );
            tartilUser.updateMemberStatus(MemberStatus.AKTIF_TAHLIL);
            tartilUser.updateProfile("Muhammad Zaki (Murid Tartil)", "081223344556", "23110091", "Teknik Informatika", "Murid aktif kelas pembinaan Tartil & Tahlil", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150");
            users.save(tartilUser);
        }

        // Demo Murid Tahfidz (Aktif di Tahfidz)
        if (!users.existsByEmail("tahfidz@pasim.ac.id")) {
            UserAccount tahfidzUser = new UserAccount(
                    "Fatimah Az-Zahra (Murid Tahfidz)",
                    "tahfidz@pasim.ac.id",
                    encoder.encode("Anggota12345678!"),
                    Role.ANGGOTA,
                    Role.ANGGOTA,
                    RegistrationStatus.APPROVED
            );
            tahfidzUser.updateMemberStatus(MemberStatus.AKTIF_TAHFIDZ);
            tahfidzUser.updateProfile("Fatimah Az-Zahra (Murid Tahfidz)", "081334455667", "23110052", "Sistem Informasi", "Murid aktif program Tahfidz Al-Qur'an intensif", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150");
            users.save(tahfidzUser);
        }

        // Demo Alumni Tahfidz (Lulus Selesai)
        if (!users.existsByEmail("alumni@pasim.ac.id")) {
            UserAccount alumniUser = new UserAccount(
                    "Ibrahim Al-Hafidz (Alumni Tahfidz)",
                    "alumni@pasim.ac.id",
                    encoder.encode("Anggota12345678!"),
                    Role.ANGGOTA,
                    Role.ANGGOTA,
                    RegistrationStatus.APPROVED
            );
            alumniUser.updateMemberStatus(MemberStatus.LULUS_TAHFIDZ);
            alumniUser.updateProfile("Ibrahim Al-Hafidz (Alumni Tahfidz)", "081445566778", "21110010", "Manajemen", "Alumni lulusan program Tahfidz Al-Qur'an UKM PASIM", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150");
            users.save(alumniUser);
        }

        // Seed core programs from business plan: Tahfidz, Tahlil, Halaqah
        Program tahfidz = null;
        Program tahlil = null;
        Program halaqah = null;

        if (programs.count() == 0) {
            tahfidz = new Program(
                    "Program Tahfidz Al-Qur'an Intensif",
                    ProgramType.TAHFIDZ,
                    "Program pembinaan hafalan Al-Qur'an mahasiswa dengan target surah, rentang ayat, dan opsi durasi fleksibel (1 pekan, 2 pekan, 3 pekan, maksimal 1 bulan)."
            );
            tahfidz.update(
                    tahfidz.getName(),
                    tahfidz.getType(),
                    tahfidz.getDescription(),
                    OffsetDateTime.of(2026, 11, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7)),
                    "Masjid Al-Muhajirin Kampus PASIM",
                    null,
                    true
            );
            tahfidz = programs.save(tahfidz);

            tahlil = new Program(
                    "Program Tahlil & Doa Bersama Mahasiswa",
                    ProgramType.TAHLIL,
                    "Kegiatan pembacaan tahlil, istighosah, dan doa bersama rutin untuk mempererat ukhuwah keagamaan mahasiswa."
            );
            tahlil.update(
                    tahlil.getName(),
                    tahlil.getType(),
                    tahlil.getDescription(),
                    OffsetDateTime.of(2026, 10, 29, 19, 30, 0, 0, ZoneOffset.ofHours(7)),
                    "Musholla Utama Lantai 2 PASIM",
                    null,
                    true
            );
            if (anggotaAccount != null) {
                tahlil.join(anggotaAccount.getId());
            }
            tahlil = programs.save(tahlil);

            halaqah = new Program(
                    "Program Halaqah Tarbiyah Mahasiswa",
                    ProgramType.HALAQAH,
                    "Pembinaan keislaman dalam kelompok belajar kecil terarah (tarbiyah, aqidah, adab penuntut ilmu) yang diikuti seluruh anggota."
            );
            halaqah.update(
                    halaqah.getName(),
                    halaqah.getType(),
                    halaqah.getDescription(),
                    OffsetDateTime.of(2026, 11, 2, 9, 0, 0, 0, ZoneOffset.ofHours(7)),
                    "Ruang Pembinaan UKM PASIM",
                    "Kelompok Bilal bin Rabah",
                    true
            );
            if (anggotaAccount != null) {
                halaqah.join(anggotaAccount.getId());
            }
            halaqah = programs.save(halaqah);

            // Seed rich materials
            materials.save(new Material(
                    tahlil,
                    "Tuntunan Bacaan Tahlil & Doa Arwah Lengkap",
                    "Susunan bacaan Tahlil lengkap meliputi: Pengantar Al-Fatihah, Surah Al-Ikhlas (3x), Surah Al-Falaq, Surah An-Nas, Awal Surah Al-Baqarah, Ayat Kursi, Istighfar, Kalimat Thayyibah (Laa ilaaha illallaah 33x), Shalawat Nabi, dan ditutup dengan doa arwah.",
                    "https://drive.google.com"
            ));

            materials.save(new Material(
                    tahlil,
                    "Materi Tahlil: Fadhilah & Adab Membaca Kalimat Thayyibah",
                    "Penjelasan mengenai makna zikir Laa ilaaha illallaah, adab menghadiri majelis tahlil, serta keutamaan menghadiahkan pahala bacaan Al-Qur'an kepada orang tua dan kaum muslimin.",
                    "https://drive.google.com"
            ));

            materials.save(new Material(
                    halaqah,
                    "Materi Halaqah 1: Adab Menuntut Ilmu & Keutamaan Al-Qur'an",
                    "Poin bahasan halaqah pertemuan pertama:\n1. Niat ikhlas karena Allah Ta'ala.\n2. Adab terhadap Al-Qur'an, majelis, dan ustadz pembimbing.\n3. Istiqomah mengulang hafalan (Muraja'ah harian).\n4. Rencana target hafalan semester berjalan.",
                    "https://drive.google.com"
            ));

            materials.save(new Material(
                    tahfidz,
                    "Panduan Tajwid & Metode Menghafal Mandiri",
                    "Panduan ringkas tajwid dasar: Makharijul Huruf, Sifatul Huruf, Hukum Nun Sukun/Tanwin, Mad dan Qashr, serta tips manajemen waktu menghafal 1 surah dalam 1 minggu, 2 minggu, atau 1 bulan.",
                    "https://drive.google.com"
            ));
        } else {
            tahlil = programs.findByTypeAndActiveTrue(ProgramType.TAHLIL).stream().findFirst().orElse(null);
            halaqah = programs.findByTypeAndActiveTrue(ProgramType.HALAQAH).stream().findFirst().orElse(null);
            tahfidz = programs.findByTypeAndActiveTrue(ProgramType.TAHFIDZ).stream().findFirst().orElse(null);
        }

        // Req 7: Seed Activities (Kegiatan UKM) e.g. 16 Desember Ada pengajian Bersama
        if (activities.count() == 0) {
            activities.save(new Activity(
                    "Pengajian Bersama & Dzikir Akbar UKM PASIM",
                    "Kajian akbar menyambut akhir semester dan doa bersama seluruh civitas akademika Universitas Nasional PASIM untuk keberkahan ilmu dan kemudahan studi.",
                    LocalDate.of(2026, 12, 16),
                    "19:30 - 21:30 WIB",
                    "Masjid Al-Muhajirin Kampus PASIM",
                    "Ustadz H. Abdul Malik, M.Ag",
                    ActivityStatus.DIJADWALKAN,
                    adminAccount
            ));

            activities.save(new Activity(
                    "Workshop Tahsin & Metode Menghafal Cepat",
                    "Pelatihan teknik makharijul huruf dan metode tikrar menghafal Al-Qur'an 1 surah dalam 1 hingga 3 pekan bersama pengurus UKM.",
                    LocalDate.of(2026, 11, 28),
                    "09:00 - 12:00 WIB",
                    "Aula Utama Lantai 3 Kampus PASIM",
                    "Ustadz Ridwan Al-Hafidz",
                    ActivityStatus.DIJADWALKAN,
                    adminAccount
            ));

            activities.save(new Activity(
                    "Malam Bina Iman dan Taqwa (MABIT) Mahasiswa",
                    "Kegiatan pembinaan ruhiyah, shalat tahajjud berjamaah, dan muhasabah diri bagi seluruh anggota UKM PASIM Mengaji.",
                    LocalDate.of(2026, 11, 14),
                    "18:00 - 06:00 WIB",
                    "Musholla Utama Lt. 2 Kampus PASIM",
                    "Tim Pembina UKM",
                    ActivityStatus.DIJADWALKAN,
                    adminAccount
            ));
        }

        // Req 2 & 10: Seed Halaqah Video Sessions
        if (halaqahSessions.count() == 0) {
            halaqahSessions.save(new HalaqahSession(
                    "Video Pembelajaran 1: Adab Menuntut Ilmu & Keutamaan Ahlul Qur'an",
                    "Materi pengantar halaqah tarbiyah wajib diikuti seluruh anggota. Simak video penjelasan hingga tuntas untuk penilaian kehadiran otomatis.",
                    "https://www.w3schools.com/html/mov_bbb.mp4",
                    30, // 30 detik untuk kemudahan preview/testing cepat
                    "Ustadz Ridwan",
                    LocalDate.of(2026, 11, 3),
                    pengurusAccount
            ));

            halaqahSessions.save(new HalaqahSession(
                    "Video Pembelajaran 2: Tahsin Tilawah & Hukum Nun Sukun/Tanwin",
                    "Kajian praktis makhraj huruf dan hukum bacaan Izhar, Idgham, Iqlab, dan Ikhfa. Dengarkan dan pelajari contoh pelafalannya.",
                    "https://www.w3schools.com/html/movie.mp4",
                    45,
                    "Ustadz Ahmad Al-Hafidz",
                    LocalDate.of(2026, 11, 10),
                    pengurusAccount
            ));
        }
    }
}
