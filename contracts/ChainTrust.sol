// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

<<<<<<< HEAD
/**
 * @title ChainTrust – DeFi Lending Protocol Credit Registry
 * @notice On-chain record of loan applications, decisions, and repayment history.
 *         All state-changing calls are restricted to the owner (the backend oracle).
 */
contract ChainTrust {

    address public owner;

    // ── Enums ──────────────────────────────────────────────────────────────

    enum LoanState { PENDING, APPROVED, DENIED, REPAID, DEFAULTED }
    enum CreditTier { REJECTED, BRONZE, SILVER, GOLD, PLATINUM }

    // ── Structs ────────────────────────────────────────────────────────────

    struct LoanRecord {
        uint256 id;
        address wallet;
        uint256 amount;           // in USD cents (no decimals needed)
        uint32  riskScoreBps;     // 0–10000 (basis points), e.g. 3500 = 35.00%
        uint32  trustScoreBps;
        CreditTier creditTier;
        LoanState  state;
        uint256 timestamp;
        bytes32 decisionHash;     // SHA-256 of wallet+amount+approved+riskScore
        string  purpose;
    }

    struct WalletStats {
        uint256 totalLoansApplied;
        uint256 totalLoansApproved;
        uint256 totalLoansRepaid;
        uint256 totalDefaulted;
        uint256 totalBorrowedUsd;  // cumulative approved amounts
        bool    blacklisted;
    }

    // ── State ──────────────────────────────────────────────────────────────

    uint256 private _nextLoanId = 1;

    mapping(address => LoanRecord[]) private _loanHistory;
    mapping(uint256 => LoanRecord)   private _loansById;
    mapping(address => WalletStats)  public  walletStats;
    mapping(address => bytes32[])    public  riskHashes;
    mapping(address => bool)         public  blacklist;

    // ── Events ─────────────────────────────────────────────────────────────

    event LoanApplied(
        uint256 indexed loanId,
        address indexed wallet,
        uint256 amount,
        CreditTier creditTier
    );
    event LoanDecided(
        uint256 indexed loanId,
        address indexed wallet,
        bool    approved,
        uint32  riskScoreBps
    );
    event LoanStateChanged(
        uint256 indexed loanId,
        address indexed wallet,
        LoanState newState
    );
    event RiskStored(address indexed wallet, bytes32 indexed riskHash);
    event BlacklistUpdated(address indexed wallet, bool blacklisted);

    // ── Modifiers ──────────────────────────────────────────────────────────

    modifier onlyOwner() {
        require(msg.sender == owner, "ChainTrust: owner only");
        _;
    }

    modifier notBlacklisted(address wallet) {
        require(!blacklist[wallet], "ChainTrust: wallet blacklisted");
        _;
    }

    // ── Constructor ────────────────────────────────────────────────────────

=======
contract ChainTrust {
    address public owner;
    mapping(address => bytes32[]) public riskHistory;
    mapping(address => bool) public blacklist;

    event RiskStored(address indexed wallet, bytes32 indexed riskHash);
    event LoanRecorded(address indexed wallet, uint256 amount, bool approved);

    modifier onlyOwner() {
        require(msg.sender == owner, "owner only");
        _;
    }

>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
    constructor() {
        owner = msg.sender;
    }

<<<<<<< HEAD
    // ── Core loan lifecycle ────────────────────────────────────────────────

    /**
     * @notice Record a new loan application and decision atomically.
     * @param wallet         Borrower address
     * @param amountUsdCents Loan amount in USD cents (e.g. 5000_00 = $5,000)
     * @param riskScoreBps   Risk score in basis points 0–10000
     * @param approved       Whether the loan was approved
     * @param tier           Credit tier enum
     * @param decisionHash   SHA-256 hash of the off-chain decision payload
     * @param purpose        Human-readable loan purpose string
     */
    function recordLoanDecision(
        address   wallet,
        uint256   amountUsdCents,
        uint32    riskScoreBps,
        bool      approved,
        CreditTier tier,
        bytes32   decisionHash,
        string    calldata purpose
    )
        external
        onlyOwner
        notBlacklisted(wallet)
        returns (uint256 loanId)
    {
        loanId = _nextLoanId++;
        LoanState state = approved ? LoanState.APPROVED : LoanState.DENIED;
        uint32 trustScoreBps = riskScoreBps <= 10000 ? uint32(10000 - riskScoreBps) : 0;

        LoanRecord memory rec = LoanRecord({
            id:            loanId,
            wallet:        wallet,
            amount:        amountUsdCents,
            riskScoreBps:  riskScoreBps,
            trustScoreBps: trustScoreBps,
            creditTier:    tier,
            state:         state,
            timestamp:     block.timestamp,
            decisionHash:  decisionHash,
            purpose:       purpose
        });

        _loanHistory[wallet].push(rec);
        _loansById[loanId] = rec;

        WalletStats storage stats = walletStats[wallet];
        stats.totalLoansApplied++;
        if (approved) {
            stats.totalLoansApproved++;
            stats.totalBorrowedUsd += amountUsdCents / 100;
        }

        emit LoanApplied(loanId, wallet, amountUsdCents, tier);
        emit LoanDecided(loanId, wallet, approved, riskScoreBps);
    }

    /**
     * @notice Update the state of an existing loan (REPAID / DEFAULTED).
     */
    function updateLoanState(uint256 loanId, LoanState newState)
        external
        onlyOwner
    {
        LoanRecord storage rec = _loansById[loanId];
        require(rec.id != 0, "ChainTrust: loan not found");
        require(rec.state == LoanState.APPROVED, "ChainTrust: only approved loans can be updated");
        require(
            newState == LoanState.REPAID || newState == LoanState.DEFAULTED,
            "ChainTrust: invalid state transition"
        );

        rec.state = newState;

        // Mirror update in history array
        for (uint i = 0; i < _loanHistory[rec.wallet].length; i++) {
            if (_loanHistory[rec.wallet][i].id == loanId) {
                _loanHistory[rec.wallet][i].state = newState;
                break;
            }
        }

        WalletStats storage stats = walletStats[rec.wallet];
        if (newState == LoanState.REPAID)    stats.totalLoansRepaid++;
        if (newState == LoanState.DEFAULTED) stats.totalDefaulted++;

        emit LoanStateChanged(loanId, rec.wallet, newState);
    }

    // ── Risk hash registry ─────────────────────────────────────────────────

    function storeRiskHash(address wallet, bytes32 riskHash) external onlyOwner {
        riskHashes[wallet].push(riskHash);
        emit RiskStored(wallet, riskHash);
    }

    // ── Blacklist ──────────────────────────────────────────────────────────

    function setBlacklist(address wallet, bool value) external onlyOwner {
        blacklist[wallet] = value;
        emit BlacklistUpdated(wallet, value);
    }

    // ── View functions ─────────────────────────────────────────────────────

    function getLoanHistory(address wallet)
        external view returns (LoanRecord[] memory)
    {
        return _loanHistory[wallet];
    }

    function getLoan(uint256 loanId)
        external view returns (LoanRecord memory)
    {
        return _loansById[loanId];
    }

    function getRiskHashes(address wallet)
        external view returns (bytes32[] memory)
    {
        return riskHashes[wallet];
    }

    function getLoanCount(address wallet)
        external view returns (uint256)
    {
        return _loanHistory[wallet].length;
=======
    function storeRiskHash(address wallet, bytes32 riskHash) external onlyOwner {
        riskHistory[wallet].push(riskHash);
        emit RiskStored(wallet, riskHash);
    }

    function recordLoan(address wallet, uint256 amount, bool approved) external onlyOwner {
        emit LoanRecorded(wallet, amount, approved);
    }

    function setBlacklist(address wallet, bool value) external onlyOwner {
        blacklist[wallet] = value;
    }

    function getRiskHistory(address wallet) external view returns (bytes32[] memory) {
        return riskHistory[wallet];
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
    }
}
