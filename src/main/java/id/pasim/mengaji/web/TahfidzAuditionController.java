package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/tahfidz")
public class TahfidzAuditionController {
    private final TahfidzAuditionRepository auditions;
    private final UserRepository users;
    private final ProgramRepository programs;
    private final HafalanTaskRepository tasks;

    // Curated bank of 1-3 random ayat for placement test (Requirement 4)
    private static final List<ApiDtos.RandomAyatView> RANDOM_AYAT_BANK = List.of(
            new ApiDtos.RandomAyatView(
                    "Al-Mulk",
                    1,
                    3,
                    "تَبَٰرَكَ ٱلَّذِى بِيَدِهِ ٱلْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَىْءٍ قَدِيرٌ ﴿١﴾ ٱلَّذِى خَلَقَ ٱلْمَوْتَ وَٱلْحَيَوٰةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ ٱلْعَزِيزُ ٱلْغَفُورُ ﴿٢﴾ ٱلَّذِى خَلَقَ سَبْعَ سَمَٰوَٰتٍ طِبَاقًا ۖ مَّا تَرَىٰ فِى خَلْقِ ٱلرَّحْمَـٰنِ مِن تَفَـٰوُتٍ ۖ فَٱرْجِعِ ٱلْبَصَرَ هَلْ تَرَىٰ مِن فُطُورٍ ﴿٣﴾",
                    "Tabaarakal-ladzii biyadihil-mulku wa huwa 'alaa kulli syai-in qadiir. Alladzii khalaqal-mauta wal-hayaata liyabluwakum ayyukum ahsanu 'amalaa, wa huwal-'aziizul-ghafuur. Alladzii khalaqa sab'a samaawaatin thibaaqaa, maa taraa fii khalqir-rahmaani min tafaawut, farji'il-bashara hal taraa min futhuur.",
                    "1. Mahasuci Allah yang menguasai (segala) kerajaan, dan Dia Mahakuasa atas segala sesuatu. 2. Yang menciptakan mati dan hidup, untuk menguji kamu, siapa di antara kamu yang lebih baik amalnya. Dan Dia Mahaperkasa, Maha Pengampun. 3. Yang menciptakan tujuh langit berlapis-lapis. Kamu tidak melihat sesuatu yang tidak seimbang pada ciptaan Tuhan Yang Maha Pengasih. Maka lihatlah sekali lagi, adakah kamu melihat sesuatu yang cacat?",
                    "Bacalah dengan tartil, perhatikan panjang pendek (mad), ghunnah, serta makharijul huruf. Rekam audio/video bacaan Anda dan cantumkan tautannya."
            ),
            new ApiDtos.RandomAyatView(
                    "Ar-Rahman",
                    1,
                    3,
                    "ٱلرَّحْمَـٰنُ ﴿١﴾ عَلَّمَ ٱلْقُرْءَانَ ﴿٢﴾ خَلَقَ ٱلْإِنسَـٰنَ ﴿٣﴾",
                    "Ar-Rahmaan. 'Allamal-Qur'aan. Khalaqal-insaan.",
                    "1. (Allah) Yang Maha Pengasih. 2. Yang telah mengajarkan Al-Qur'an. 3. Dia menciptakan manusia.",
                    "Bacalah dengan tenang dan perhatikan kejelasan makhraj huruf Ra, Haa', dan Qaf."
            ),
            new ApiDtos.RandomAyatView(
                    "An-Naba'",
                    1,
                    3,
                    "عَمَّ يَتَسَآءَلُونَ ﴿١﴾ عَنِ ٱلنَّبَإِ ٱلْعَظِيمِ ﴿٢﴾ ٱلَّذِى هُمْ فِيهِ مُخْتَلِفُونَ ﴿٣﴾",
                    "'Amma yatasaa'aluun. 'Anin-naba'il-'azhiim. Alladzii hum fiihi mukhtalifuun.",
                    "1. Tentang apakah mereka saling bertanya-tanya? 2. Tentang berita yang besar (hari kebangkitan), 3. yang dalam hal itu mereka berselisih.",
                    "Perhatikan ghunnah musyaddadah pada mim bertasydid 'Amma dan mad wajib muttashil pada yatasaa'aluun."
            ),
            new ApiDtos.RandomAyatView(
                    "Al-Waqi'ah",
                    1,
                    3,
                    "إِذَا وَقَعَتِ ٱلْوَاقِعَةُ ﴿١﴾ لَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ ﴿٢﴾ خَافِضَةٌ رَّافِعَةٌ ﴿٣﴾",
                    "Idzaa waqa'atil-waaqi'ah. Laisa liwaq'atihaa kaadzibah. Khaafidhatur-raafi'ah.",
                    "1. Apabila terjadi hari Kiamat, 2. terjadinya tidak dapat didustakan (pasti terjadi). 3. (Kejadian itu) merendahkan (satu golongan) dan meninggikan (golongan yang lain).",
                    "Perhatikan makhraj dzal pada Idzaa dan idgham bighunnah pada khaafidhatur-raafi'ah."
            ),
            new ApiDtos.RandomAyatView(
                    "Al-Kahf",
                    1,
                    3,
                    "ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَـٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَا ۜ ﴿١﴾ قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ ٱلْمُؤْمِنِينَ ٱلَّذِينَ يَعْمَلُونَ ٱلصَّـٰلِحَـٰتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا ﴿٢﴾ مَّـٰكِثِينَ فِيهِ أَبَدًا ﴿٣﴾",
                    "Alhamdulillaahil-ladzii anzala 'alaa 'abdihil-kitaaba wa lam yaj'al lahuu 'iwajaa. Qayyimal liyundzira ba'san syadiidam mil ladunhu wa yubasysyiral-mu'miniinal-ladziina ya'maluunash-shaalihaati anna lahum ajran hasanaa. Maakitsiina fiihi abadaa.",
                    "1. Segala puji bagi Allah yang telah menurunkan Kitab (Al-Qur'an) kepada hamba-Nya dan Dia tidak menjadikannya bengkok; 2. sebagai bimbingan yang lurus, untuk memperingatkan siksaan yang sangat pedih dari sisi-Nya dan memberikan kabar gembira kepada orang-orang mukmin yang mengerjakan kebajikan bahwa mereka akan mendapat balasan yang baik; 3. mereka kekal di dalamnya untuk selama-lamanya.",
                    "Perhatikan saktah pada kata 'iwajaa (berhenti sejenak tanpa mengambil nafas) dan ikhfa haqiqi pada liyundzira."
            )
    );

    public TahfidzAuditionController(TahfidzAuditionRepository auditions, UserRepository users, ProgramRepository programs, HafalanTaskRepository tasks) {
        this.auditions = auditions;
        this.users = users;
        this.programs = programs;
        this.tasks = tasks;
    }

    // Req 4: Get randomized 1-3 ayat test challenge
    @GetMapping("/test/random-ayat")
    public ApiDtos.RandomAyatView getRandomAyat() {
        int index = new Random().nextInt(RANDOM_AYAT_BANK.size());
        return RANDOM_AYAT_BANK.get(index);
    }

    // Current member's audition state
    @GetMapping("/my-audition")
    public ApiDtos.TahfidzAuditionView getMyAudition(HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        return auditions.findFirstByMember_IdOrderByCreatedAtDesc(actor.getId())
                .map(ApiDtos.TahfidzAuditionView::of)
                .orElse(null);
    }

    // Req 4: Anggota submits the 1-3 ayat reading recording
    @PostMapping("/test/submit")
    @Transactional
    public ApiDtos.TahfidzAuditionView submitTest(@Valid @RequestBody ApiDtos.TahfidzTestSubmitRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ANGGOTA);

        // Verification: Member must have completed Tahlil or be permitted to take test
        if (actor.getMemberStatus() == MemberStatus.AKTIF_TAHLIL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Harap selesaikan program Tahlil terlebih dahulu sebelum mengikuti tes Tahfidz");
        }

        int ayatCount = req.ayatEnd() - req.ayatStart() + 1;
        if (req.ayatStart() < 1 || ayatCount < 1 || ayatCount > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tes bacaan Tahfidz wajib terdiri dari 1 sampai 3 ayat (maksimal 3 ayat)");
        }

        var audition = new TahfidzAudition(
                actor,
                req.surah().trim(),
                req.ayatStart(),
                req.ayatEnd(),
                req.recordingUrl().trim(),
                req.memberNote()
        );
        actor.updateMemberStatus(MemberStatus.MENUNGGU_TES_TAHFIDZ);
        users.save(actor);

        return ApiDtos.TahfidzAuditionView.of(auditions.save(audition));
    }

    // Pengurus lists all audition submissions
    @GetMapping("/auditions")
    public List<ApiDtos.TahfidzAuditionView> listAuditions(HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);
        return auditions.findAllByOrderByCreatedAtDesc().stream().map(ApiDtos.TahfidzAuditionView::of).toList();
    }

    // Pengurus reviews the 1-3 ayat reading test
    @PatchMapping("/auditions/{id}/review")
    @Transactional
    public ApiDtos.TahfidzAuditionView reviewAudition(@PathVariable UUID id, @Valid @RequestBody ApiDtos.AuditionReviewRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);

        var audition = auditions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        audition.review(actor, req.status(), req.reviewNote());

        var member = audition.getMember();
        if (req.status() == AuditionStatus.DISETUJUI) {
            // Member is approved to pick duration and target surah/ayat
            member.updateMemberStatus(MemberStatus.MEMILIH_TARGET_TAHFIDZ);
        } else {
            member.updateMemberStatus(MemberStatus.LULUS_TAHLIL); // Can retry test
        }
        users.save(member);

        return ApiDtos.TahfidzAuditionView.of(auditions.save(audition));
    }

    // Req 5: Member chooses target duration (1 week, 2 week, 3 week, 1 month max) and surah/ayat
    @PostMapping("/target/submit")
    @Transactional
    public ApiDtos.TahfidzAuditionView submitTarget(@Valid @RequestBody ApiDtos.TahfidzTargetSubmitRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ANGGOTA);

        if (!List.of(7, 14, 21, 30).contains(req.chosenDurationDays())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pilihan durasi harus 1 pekan (7 hari), 2 pekan (14 hari), 3 pekan (21 hari), atau 1 bulan (30 hari)");
        }

        var audition = auditions.findFirstByMember_IdOrderByCreatedAtDesc(actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Riwayat tes Tahfidz tidak ditemukan"));

        if (audition.getStatus() != AuditionStatus.DISETUJUI) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tes bacaan Al-Qur'an Anda harus disetujui pengurus terlebih dahulu");
        }

        audition.chooseTarget(req.targetSurah().trim(), req.targetAyatStart(), req.targetAyatEnd(), req.chosenDurationDays());
        actor.updateMemberStatus(MemberStatus.MENUNGGU_APPROVAL_DURASI);
        users.save(actor);

        return ApiDtos.TahfidzAuditionView.of(auditions.save(audition));
    }

    // Req 5: Pengurus approves chosen target duration and surah -> creates active HafalanTask!
    @PatchMapping("/auditions/{id}/target-review")
    @Transactional
    public ApiDtos.TahfidzAuditionView reviewTarget(@PathVariable UUID id, @Valid @RequestBody ApiDtos.AuditionReviewRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);

        var audition = auditions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (audition.getChosenDurationDays() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anggota belum memilih durasi target hafalan");
        }

        audition.reviewTarget(actor, req.status(), req.reviewNote());
        var member = audition.getMember();

        if (req.status() == AuditionStatus.DISETUJUI) {
            // Find Tahfidz program
            var tahfidzProgram = programs.findByTypeAndActiveTrue(ProgramType.TAHFIDZ).stream().findFirst().orElse(null);
            if (tahfidzProgram == null) {
                tahfidzProgram = programs.findAllByOrderByCreatedAtDesc().stream()
                        .filter(p -> p.getType() == ProgramType.TAHFIDZ)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Program Tahfidz belum tersedia"));
            }

            // Create official HafalanTask for member
            var task = new HafalanTask(
                    tahfidzProgram,
                    member,
                    audition.getTargetSurah(),
                    audition.getTargetAyatStart(),
                    audition.getTargetAyatEnd(),
                    String.valueOf(audition.getChosenDurationDays()),
                    LocalDate.now(),
                    "Target hafalan disetujui pengurus: " + audition.getTargetSurah() + " (" + audition.getTargetAyatStart() + "-" + audition.getTargetAyatEnd() + ") durasi " + audition.getChosenDurationDays() + " hari.",
                    actor
            );
            task.chooseDuration(audition.getChosenDurationDays());
            tasks.save(task);

            member.updateMemberStatus(MemberStatus.AKTIF_TAHFIDZ);
        } else {
            member.updateMemberStatus(MemberStatus.MEMILIH_TARGET_TAHFIDZ);
        }
        users.save(member);

        return ApiDtos.TahfidzAuditionView.of(auditions.save(audition));
    }
}
